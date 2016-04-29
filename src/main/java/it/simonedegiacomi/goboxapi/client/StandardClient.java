package it.simonedegiacomi.goboxapi.client;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.neovisionaries.ws.client.WebSocketException;
import it.simonedegiacomi.goboxapi.GBCache;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxapi.utils.UDPUtils;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Phaser;

/**
 * This is an implementation of the gobox api client interface. This
 * client uses WebSocket to transfer the file list, to authenticate
 * and to share events and use HTTP(s) to transfer the files.
 *
 * @author Degiacomi Simone
 * Created on 31/12/2015.
 */
public class StandardClient extends Client {

    private static final int DEFAULT_PORT = 5406;

    public enum ConnectionMode { BRIDGE_MODE, DIRECT_MODE, LOCAL_DIRECT_MODE };

    private static final Logger log = Logger.getLogger(StandardClient.class.getName());

    /**
     * Object used to create the urls.
     */
    private static URLBuilder urls = new URLBuilder();

    /**
     * WebSocket connection to the server
     */
    private MyWSClient server;

    /**
     * Authorization object used to make the call
     */
    private final Auth auth;

    /**
     * Gson instance to create json objects
     */
    private final Gson gson = new MyGsonBuilder().create();

    /**
     * State of the client
     */
    private ClientState state = ClientState.NOT_READY;

    /**
     * Modality of current connection.
     */
    private ConnectionMode mode = ConnectionMode.BRIDGE_MODE;

    /**
     * Set of events to ignore
     */
    private final Set<String> eventsToIgnore = new HashSet<>();

    /**
     * Cache of the files information
     */
    private final GBCache cache = new GBCache();

    private DisconnectedListener disconnectedListener;

    private Phaser works = new Phaser();

    public static void setUrlBuilder (URLBuilder builder) {
        urls = builder;
    }

    /**
     * Construct a sync object, but first try to login to gobox.
     *
     * @param auth Auth object that will be used to authenticate
     */
    public StandardClient(final Auth auth) {
        this.auth = auth;
    }

    /**
     * Interface for the onDisconnect event
     */
    public interface DisconnectedListener {
        public void onDisconnect();
    }

    /**
     * Set the listener for the disconnection cause by the websocket
     * @param listener Listener to call
     */
    public void onDisconnect (DisconnectedListener listener) {
        this.disconnectedListener = listener;
    }

    /**
     * Check if the client is connected to the storage
     * @return
     */
    @Override
    public boolean isReady() {
        return state == ClientState.READY;
    }

    @Override
    public ClientState getState () { return state; }

    /**
     * Connect to the server and to the storage. This method will block
     * the thread util the websocket connection is estabilite and the
     * storage info event received
     */
    @Override
    public void init() throws ClientException {
        if(state != ClientState.NOT_READY)
            throw new ClientException("Client already connected");

        // Change the current state
        state = ClientState.INIT;

        try {

            // Create the websocket client
            server = new MyWSClient(urls.getURI("socketClient"));
        } catch (IOException ex) {
            throw new ClientException(ex.toString());
        }

        // Authorize the connection
        auth.authorizeWs(server);

        // When the webSocket in opened, send the authentication object
        server.onEvent("open", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {

                // Change current state
                state = ClientState.READY;

                // TODO: Send network info
                if(onConnectListener != null)
                    onConnectListener.onEvent(null);
            }
        });

        server.onEvent("error", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {

                // Change state
                state = ClientState.NOT_READY;

                // Call disconnect listener
                disconnectedListener.onDisconnect();
            }
        });

        final CountDownLatch readyCountDown = new CountDownLatch(1);
        try {
            // Register the storageInfo event
            server.onEvent("storageInfo", new WSEventListener() {
                @Override
                public void onEvent(JsonElement data) {
                    // Remove the storage info listener
                    server.removeListener("storageInfo");
                    readyCountDown.countDown();
                }
            });

            // Connect
            server.connect();

            readyCountDown.await();
        } catch (WebSocketException ex) {
            ex.printStackTrace();
            throw new ClientException(ex.toString());
        } catch (InterruptedException ex) {
            throw new ClientException("Storage event info not received");
        }
    }

    private WSEventListener onConnectListener;

    public void setOnConnectListener (WSEventListener listener) {
        this.onConnectListener = listener;
    }

    /**
     * Download a file from the storage copying the file to the output stream.
     * When the download is complete the stream is closed.
     *
     * @param file File to download.
     * @param dst  Output stream where put the content of the file.
     * @throws ClientException Error during the download
     */
    @Override
    public void getFile(GBFile file, OutputStream dst) throws ClientException, IOException {
        works.register();
        try {
            // Create and fill the request object
            JsonObject request = new JsonObject();
            request.addProperty("ID", file.getID());

            URL url = urls.get("getFile", request);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            // Authorize the connection
            auth.authorize(conn);

            InputStream fromServer = conn.getInputStream();

            // Copy the file
            ByteStreams.copy(conn.getInputStream(), dst);

            // Close the connection
            fromServer.close();
            conn.disconnect();
            dst.close();

        } catch (IOException ex) {
            works.arriveAndDeregister();
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
        works.arriveAndDeregister();
    }

    /**
     * Create a new directory in the storage. This method also ignore the event
     * incoming from the storage that advice the other clients about this new folder
     *
     * @param newDir Directory to create
     * @throws ClientException
     */
    @Override
    public void createDirectory(GBFile newDir) throws ClientException {
        try {
            // Ignore the events from the server related to this file
            eventsToIgnore.add(newDir.getPathAsString());
            FutureTask<JsonElement> future = server.makeQuery("createFolder", gson.toJsonTree(newDir, GBFile.class));
            JsonObject response = future.get().getAsJsonObject();
            newDir.setID(response.get("newFolderId").getAsLong());
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    /**
     * This method retrieve the information about the specified file. This method also
     * use an internal cache, so you can call this method multiple times without generating
     * useless network traffic.
     *
     * @param father File to look at
     * @return New GBFile with the information of the storage
     * @throws ClientException
     */
    @Override
    public GBFile getInfo(GBFile father) throws ClientException {
        // Check if the file is already cached
        GBFile fromCache = cache.get(father);
        if(fromCache != null)
            return fromCache;
        try {
            JsonObject request = new JsonObject();
            request.add("file", gson.toJsonTree(father, GBFile.class));
            request.addProperty("findPath", true);
            request.addProperty("findChildren", true);

            JsonObject response = server.makeQuery("info", request).get().getAsJsonObject();
            boolean found = response.get("found").getAsBoolean();
            if (!found)
                return null;
            GBFile detailedFile = gson.fromJson(response.get("file"), GBFile.class);
            // cache the file
            cache.add(detailedFile);
            return detailedFile;
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    /**
     * Upload the file to the server reading his content from the input stream passed as
     * argument. This method also ignore the generated event sent by the storage to the other
     * clients.
     *
     * @param file   File to send File to send. The object must have or the field father id or the path.
     * @param stream Stream of the file Stream that will be sent to the storage
     * @throws ClientException
     */
    @Override
    public void uploadFile(GBFile file, InputStream stream) throws ClientException, IOException {
        try {
            eventsToIgnore.add(file.getPathAsString());

            // Get the url to upload the file
            URL url = urls.get("uploadFile", gson.toJsonTree(file), true);

            // Create a new https connection
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            // Authorize it
            auth.authorize(conn);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Length", String.valueOf(file.getSize()));

            OutputStream toStorage = conn.getOutputStream();
            // Send the file
            ByteStreams.copy(stream, toStorage);
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new ClientException("Response code of the upload: " + responseCode);
            }

            // Close the http connection
            toStorage.close();
            conn.disconnect();
            stream.close();
        } catch (ProtocolException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void trashFile(GBFile file, boolean toTrash) throws ClientException {

        // Prepare the request
        JsonObject request = new JsonObject();
        request.addProperty("toTrash", toTrash);
        request.add("file", gson.toJsonTree(file, GBFile.class));

        try {
            JsonObject response = server.makeQuery("trashFile", request).get().getAsJsonObject();
            if (!response.get("success").getAsBoolean()) {
                throw new ClientException(response.get("error").getAsString());
            }
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    /**
     * Remove the file from the storage and ignore the event generated from this action.
     *
     * @param file File to remove
     * @throws ClientException
     */
    @Override
    public void removeFile(GBFile file) throws ClientException {
        eventsToIgnore.add(file.getPathAsString());
        // Make the request trough handlers socket
        try {
            JsonObject res = server.makeQuery("removeFile", gson.toJsonTree(file, GBFile.class)).get().getAsJsonObject();
            if (!res.get("success").getAsBoolean()) {
                throw new ClientException(res.get("error").getAsString());
            }
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    /**
     * Set the listener for the event from the storage.
     * Id another listener is already set, the listener
     * will be replaced
     *
     * @param listener Listener that will called with the relative
     */
    public void setSyncEventListener(final SyncEventListener listener) {

        // If the listener passed is null, remove the old listener
        // (or do nothing if was never set)
        if (listener == null)
            server.removeListener("syncEvent");

        // Add a new listener onEvent the handlers socket
        server.onEvent("syncEvent", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {

                // Wrap the data in a new SyncEvent
                SyncEvent event = gson.fromJson(data, SyncEvent.class);

                // Check if this is the notification for a event that i've generated.
                if (eventsToIgnore.remove(event.getRelativeFile().getPathAsString()))
                    // Because i've generated this event, i ignore it
                    return;

                // And call the listener
                listener.on(event);
            }
        });
    }

    @Override
    public void requestEvents(long lastHeardId) {
        JsonObject request = new JsonObject();
        request.addProperty("ID", lastHeardId);
        server.sendEvent("getEventsList", request);
    }

    @Override
    public void shutdown() throws ClientException {
        if(server == null || !server.isConnected())
            throw new ClientException("Client not connected");
    }

    @Override
    public List<GBFile> getSharedFiles() throws ClientException {
        try {
            JsonObject response = server.makeQuery("getSharedFiles", new JsonObject()).get().getAsJsonObject();
            return gson.fromJson(response.get("files"), new TypeToken<List<GBFile>>(){}.getType());
        } catch (InterruptedException ex) {

        } catch (ExecutionException ex) {

        }
        return null;
    }

    @Override
    public void share(GBFile file, boolean share) throws ClientException {
        JsonObject request = new JsonObject();
        request.addProperty("share", share);
        request.addProperty("ID", file.getID());
        try {
            JsonObject res = server.makeQuery("share", request).get().getAsJsonObject();
            if (!res.get("success").getAsBoolean()) {
                throw new ClientException(res.get("error").getAsString());
            }
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public List<GBFile> getFilesByFilter(GBFilter filter) throws ClientException {

        JsonElement request = gson.toJsonTree(filter, GBFilter.class);
        try {
            JsonObject response = server.makeQuery("search", request).get().getAsJsonObject();
            if(response.get("error").getAsBoolean())
                return null;
            return gson.fromJson(response.get("result"), new TypeToken<List<GBFile>>(){}.getType());
        } catch (InterruptedException ex) {

        } catch (ExecutionException ex) {

        }
        return null;
    }

    /**
     * Request to the server the list of the recent files
     * @param from Offset of the result list
     * @param size Limit of the result list
     * @return List of recent files
     * @throws ClientException
     */
    @Override
    public List<GBFile> getRecentFiles(long from, long size) throws ClientException {

        // Prepare the request
        JsonObject request = new JsonObject();

        request.addProperty("from", from);
        request.addProperty("size", size);

        try {
            JsonObject response = server.makeQuery("recent", request).get().getAsJsonObject();

            // Check if there was an error
            if(response.get("error").getAsBoolean())
                return null;
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }
        return null;
    }

    /**
     * Return the list of the trashed files
     * @return List of the trashed files
     * @throws ClientException
     */
    @Override
    public List<GBFile> getTrashedFiles() throws ClientException {
        try {
            JsonObject response = server.makeQuery("trashed", new JsonObject()).get().getAsJsonObject();

            // Check if there was an error
            if(response.get("success").getAsBoolean())
                throw new ClientException(response.get("error").getAsString());
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
        return null;
    }

    @Override
    public void emptyTrash() throws ClientException {
        try {
            JsonObject res = server.makeQuery("emptyTrash", new JsonObject()).get().getAsJsonObject();
            if (!res.get("success").getAsBoolean()) {
                throw new ClientException(res.get("error").getAsString());
            }
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
        }
    }


    // TODO: remove this method
    public void shutdownSync () throws ClientException {
        if(server == null || !server.isConnected())
            throw new ClientException("Client not connected");
        works.arriveAndAwaitAdvance();
    }

    /**
     * Try to switch to the selected mode. This method will block the thread.
     * If you try to switch to the current mode, nothing happen.
     * @param nextMode Next mode to switch
     * @throws ClientException
     */
    public void switchMode (ConnectionMode nextMode) throws ClientException {
        // Check if the client is connected
        if(server == null && !server.isConnected())
            throw new IllegalStateException("client not connected");

        // Assert that is not the current mode
        if(nextMode == mode)
            return;

        switch (nextMode) {
            case BRIDGE_MODE:

                // Just change the flag to return in bridge mode
                this.mode = ConnectionMode.BRIDGE_MODE;
                break;

            case DIRECT_MODE:

                // Call the right method
                switchToDirectMode();
                return;
        }
    }

    /**
     * Switch to direct mode. First attempt with the local connection, sending
     * a UDP packet, then fallback to internet direct connection. If the switch happens
     * true is return, otherwise false or a new exception will be thrown.
     * This method will block the thread until the switch is completed
     * @return True if the switch happen
     * @throws ClientException Exception during the switch
     */
    private boolean switchToDirectMode () throws ClientException {
        try {
            URL ip = null;

            // Ask the storage if this modality is available
            JsonObject response = server.makeQuery("directLogin", null).get().getAsJsonObject();

            // Get the ip
            String publicIp = response.get("publicIP").getAsString();
            String port = response.get("port").getAsString();
            ip = new URL("https://" + publicIp + ':' + port);

            // Try with the local mode
            // Prepare the request packet
            String requestString = "GOBOX_DIRECT_" + auth.getUsername();
            byte[] requestBytes = requestString.getBytes();

            // Send the request trough UDP
            UDPUtils.sendBroadcastPacket(DEFAULT_PORT, requestBytes);

            boolean local = false;

            // Try to receive the response
            try {
                DatagramPacket udpResponse = UDPUtils.receive();

                // ok, some response ...
                String data = udpResponse.getData().toString().trim();
                if (data.startsWith("GOBOX_DIRECT_PORT")) {
                    ip = new URL("https://" + udpResponse.getAddress().toString() + ':' + port);
                }
            } catch (SocketTimeoutException ex) {
                // No response or some error, fallback with bridge
                log.info("direct local connection failed");
            }

            // Make the switch
            HttpsURLConnection conn = (HttpsURLConnection) ip.openConnection();
            int code = conn.getResponseCode();
            conn.disconnect();

            if (code != 200)
                return false;

            mode = local ? ConnectionMode.DIRECT_MODE : ConnectionMode.LOCAL_DIRECT_MODE;

            return true;
        } catch (Exception ex) {
            log.info(ex.toString(), ex);
        }
        return false;
    }
}