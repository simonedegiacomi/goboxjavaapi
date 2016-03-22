package it.simonedegiacomi.goboxapi.client;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.neovisionaries.ws.client.WebSocketException;
import it.simonedegiacomi.goboxapi.GBCache;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.WSQueryResponseListener;
import it.simonedegiacomi.goboxapi.utils.EasyHttps;
import it.simonedegiacomi.goboxapi.utils.EasyHttpsException;
import it.simonedegiacomi.goboxapi.utils.UDPUtils;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import it.simonedegiacomi.storage.UDPStorageServer;
import it.simonedegiacomi.utils.MyGsonBuilder;
import org.apache.log4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.HashSet;
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

    public enum ConnectionMode { BRIDGE_MODE, DIRECT_MODE };

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
    public StandardClient(final Auth auth) throws ClientException {
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
    public boolean isOnline() {
        return server.isConnected();
    }

    /**
     * Connect to the server and to the storage. This method will block
     * the thread util the websocket connection is estabilite and the
     * storage info event received
     */
    public void connect() throws IOException, ClientException {

        if(server != null && server.isConnected())
            throw new ClientException("Client already connected");

        // Create the websocket client
        server = new MyWSClient(urls.getURI("socketClient"));

        // Authorize the connection
        auth.authorizeWs(server);

        // When the webSocket in opened, send the authentication object
        server.onEvent("open", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {
                // TODO: Send network info
            }
        });

        server.onEvent("error", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {
                disconnectedListener.onDisconnect();
            }
        });

        CountDownLatch readyCountDown = new CountDownLatch(1);
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

        } catch (Exception ex) {
            works.arriveAndDeregister();
            log.warn(ex.toString());
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
            JsonObject response = (JsonObject) future.get();
            newDir.setID(response.get("newFolderId").getAsLong());
        } catch (InterruptedException ex) {
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
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

            JsonObject response = (JsonObject) server.makeQuery("info", request).get();
            boolean found = response.get("found").getAsBoolean();
            if (!found)
                return null;
            GBFile detailedFile = gson.fromJson(response.get("file"), GBFile.class);
            // cache the file
            cache.add(detailedFile);
            return detailedFile;
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
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
            server.makeQuery("removeFile", gson.toJsonTree(file), new WSQueryResponseListener() {
                @Override
                public void onResponse(JsonElement response) {
                    log.info("File deletion response: " + response.toString());
                }
            });
        } catch (Exception ex) {
            log.warn(ex.toString());
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void updateFile(GBFile file, InputStream file2) {

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
        server.sendEvent("getEventsList", request, false);
    }

    @Override
    public void shutdown() throws ClientException {
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
            throw new ClientException("Client not connected");

        // Assert that is not the current mode
        if(nextMode == mode)
            return;

        switch (nextMode) {
            case BRIDGE_MODE:
                // Just change the flag
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

        // IP of the server for the direct connection
        URL ip = null;

        /**
         * First attempt with the local network
         */
        try {

            // Prepare the request packet
            String requestString = "GOBOX_DIRECT_" + auth.getUsername();
            byte[] requestBytes = requestString.getBytes();

            // Send the request trough UDP
            UDPUtils.sendBroadcastPacket(UDPStorageServer.DEFAULT_PORT, requestBytes);

            try {
                DatagramPacket response = UDPUtils.receive();

                // ok, some response ...
                String data = response.getData().toString().trim();
                if (data.startsWith("GOBOX_DIRECT_PORT")) {
                    int port = Integer.parseInt(data.split(":")[1]);
                    ip = new URL("https://" + response.getAddress().toString() + ':' + port);
                }


            } catch (SocketTimeoutException ex) {
                // no response... try with the internet...
            }


        } catch (Exception ex) {
            // try with the internet...
        }

        if(ip == null) {
            /**
             * Direct connection trough internet
             */
            try {

                // Ask the storage if this modaliity is available
                JsonObject response = server.makeQuery("directLogin", null).get().getAsJsonObject();

                // Try the internet connection
                String publicIp = response.get("publicIP").getAsString();
                String port = response.get("port").getAsString();
                ip = new URL("https://" + publicIp + ':' + port);

            } catch (Exception ex) {
                throw new ClientException("Switch failed");
            }
        }

        try {
            /**
             * Make the https request
             */
            JsonObject request = new JsonObject();
            JsonObject response = (JsonObject) EasyHttps.post(ip, request, null);

            boolean success = response.get("success").getAsBoolean();

            if (success) {
                mode = ConnectionMode.DIRECT_MODE;
                return true;
            }

        } catch (EasyHttpsException ex) {

        } catch (IOException ex) {

        }
        return false;
    }
}