package it.simonedegiacomi.goboxapi.client;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import it.simonedegiacomi.goboxapi.GBCache;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.myws.WSException;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import org.apache.log4j.Logger;

import javax.net.ssl.*;
import java.io.*;
import java.net.ProtocolException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * This is an implementation of the gobox api client interface. This client uses WebSocket to transfer the file list,
 * to authenticate and to share events and use HTTP(s) to transfer the files.
 *
 * @author Degiacomi Simone
 *         Created on 31/12/2015.
 */
public class StandardGBClient extends GBClient {

    public enum ConnectionMode {
        BRIDGE_MODE,
        DIRECT_MODE,
        LOCAL_DIRECT_MODE
    }

    /**
     * Logger of the class
     */
    private static final Logger log = Logger.getLogger(StandardGBClient.class);

    /**
     * Object used to create the urls.
     */
    private static URLBuilder urls = URLBuilder.DEFAULT;

    /**
     * WebSocket connection to the server
     */
    private MyWSClient server;

    /**
     * Authorization object used to make the call
     */
    private final GBAuth auth;

    /**
     * Gson instance to create json objects
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * State of the client
     */
    private ClientState state = ClientState.NOT_READY;

    /**
     * Set of events to ignore
     */
    private final Set<String> eventsToIgnore = new HashSet<>();

    /**
     * Cache of the files information
     */
    private final GBCache cache = new GBCache();

    /**
     * Set of sync event listeners
     */
    private final Set<SyncEventListener> listeners = new HashSet<>();

    /**
     * Filter echo sync event
     */
    private boolean filterEcho = false;

    /**
     * Listener for the disconnection
     */
    private DisconnectedListener disconnectedListener;

    /**
     * Object that contains the transfer information
     */
    private TransferProfile currentTransferProfile;

    /**
     * Construct a sync object, but first try to login to gobox.
     *
     * @param GBAuth GBAuth object that will be used to authenticate
     */
    public StandardGBClient(final GBAuth GBAuth) {
        this.auth = GBAuth;
        this.currentTransferProfile = new TransferProfile(urls, GBAuth);
    }

    /**
     * Interface for the onDisconnect event
     */
    public interface DisconnectedListener {
        public void onDisconnect();
    }

    /**
     * Set the listener for the disconnection cause by the websocket
     *
     * @param listener Listener to call
     */
    public void onDisconnect(DisconnectedListener listener) {
        this.disconnectedListener = listener;
    }

    /**
     * Check if the client is connected to the storage
     *
     * @return true if the client is ready to use
     */
    @Override
    public boolean isReady() {
        return state == ClientState.READY;
    }

    @Override
    public ClientState getState() {
        return state;
    }

    /**
     * Connect to the server and to the storage. This method will block
     * the thread util the websocket connection is estabilite and the
     * storage info event received
     */
    @Override
    public boolean init() throws ClientException {
        if (state != ClientState.NOT_READY)
            throw new ClientException("GBClient already connected");

        // Change the current state
        state = ClientState.INITIALIZING;

        try {

            // Create the websocket client
            server = new MyWSClient(urls.getURI("socketClient"));
        } catch (IOException ex) {
            throw new ClientException(ex.toString());
        }

        // Authorize the connection
        auth.authorize(server);

        // When the webSocket in opened, send the authentication object
        server.onEvent("open", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {

                log.info("Websocket connected");
            }
        });

        server.onEvent("error", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {
                log.info("websocket error");

                // If the client was ready
                if (state == ClientState.READY) {

                    // Call disconnect listener
                    disconnectedListener.onDisconnect();
                }

                // Change state
                state = ClientState.NOT_READY;
            }
        });

        server.onEvent("close", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {
                log.info("websocket closed");

                // If the client was ready
                if (state == ClientState.READY) {

                    // Call disconnect listener
                    disconnectedListener.onDisconnect();
                }

                // Change state
                state = ClientState.NOT_READY;
            }
        });

        final CountDownLatch readyCountDown = new CountDownLatch(1);
        try {
            // Register the storageInfo event
            server.onEvent("storageInfo", new WSEventListener() {
                @Override
                public void onEvent(JsonElement data) {

                    if (data.getAsJsonObject().get("connected").getAsBoolean()) {
                        log.info("Storage connected");

                        // Change current state
                        state = ClientState.READY;
                        registerSyncEventListener();
                        readyCountDown.countDown();
                        return;
                    }
                    log.info("Storage not connected");
                    state = ClientState.NOT_READY;

                    if (readyCountDown.getCount() > 0) {
                        readyCountDown.countDown();
                        return;
                    }

                    if (disconnectedListener != null)
                        disconnectedListener.onDisconnect();
                }
            });

            // Connect
            server.connect();

            readyCountDown.await();

            return isReady();
        } catch (WSException ex) {
            ex.printStackTrace();
            throw new ClientException(ex.toString());
        } catch (InterruptedException ex) {
            throw new ClientException("Storage event info not received");
        }
    }

    private void registerSyncEventListener() {
        // Add a new listener onEvent the handlers socket
        server.onEvent("syncEvent", new WSEventListener() {
            @Override
            public void onEvent(JsonElement data) {

                // Wrap the data in a new SyncEvent
                SyncEvent event = gson.fromJson(data, SyncEvent.class);

                // Check if this is the notification for a event that i've generated.
                if (eventsToIgnore.remove(event.getRelativeFile().getPathAsString()) && filterEcho)
                    // Because i've generated this event, i ignore it
                    return;

                // And call all the listeners
                for (SyncEventListener listener : listeners)
                    listener.on(event);
            }
        });
    }

    @Override
    public URL getUrl(TransferProfile.Action action, GBFile file, boolean preview) {
        JsonObject params = new JsonObject();
        params.addProperty("ID", file.getID());
        params.addProperty("preview", preview);
        return currentTransferProfile.getUrl(action, params);
    }

    /**
     * Download a file from the storage copying the file to the output stream.
     * If you call this method with a file that is a folder, you'll get a compressed version of the folder
     * This method close the destination stream.
     *
     * @param file File to download.
     * @param dst  Output stream where put the content of the file.
     * @throws ClientException Error during the download
     */
    @Override
    public void getFile(GBFile file, OutputStream dst) throws ClientException, IOException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");
        try {
            // Create and fill the request object
            JsonObject request = new JsonObject();
            request.addProperty("ID", file.getID());

            // Open the connection
            HttpsURLConnection conn = currentTransferProfile.openConnection(TransferProfile.Action.DOWNLOAD, request, false);

            InputStream fromServer = conn.getInputStream();

            // Copy the file
            ByteStreams.copy(conn.getInputStream(), dst);

            // Close the connection
            fromServer.close();
            conn.disconnect();
            dst.close();
        } catch (IOException ex) {
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
     * @throws ClientException Error uploading the file
     * @throws IOException Stream error
     */
    @Override
    public void uploadFile(GBFile file, InputStream stream) throws ClientException, IOException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");
        if (file.isDirectory())
            throw new InvalidParameterException("this file is a folder");

        try {
            eventsToIgnore.add(file.getPathAsString());

            JsonObject req = gson.toJsonTree(file, GBFile.class).getAsJsonObject();

            // Create a new https connection
            HttpsURLConnection conn = currentTransferProfile.openConnection(TransferProfile.Action.UPLOAD, req, true);
            conn.setDoInput(true);
            conn.setDoOutput(true);

            // Prepare the connection
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

    /**
     * Create a new directory in the storage. This method also ignore the event
     * incoming from the storage that advice the other clients about this new folder
     *
     * @param newDir Directory to create
     * @throws ClientException
     */
    @Override
    public void createDirectory(GBFile newDir) throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

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
     * @throws ClientException client error
     */
    @Override
    public GBFile getInfo(GBFile father) throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

        // Check if the file is already cached
        GBFile fromCache = cache.get(father);
        if (fromCache != null)
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

    @Override
    public void trashFile(GBFile file, boolean toTrash) throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

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
    public void removeFile (GBFile file) throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

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
     * Add the listener for the event from the storage.
     *
     * @param listener Listener that will called with the relative
     */
    public void addSyncEventListener (final SyncEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeSyncEventListener (SyncEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void shutdown() throws ClientException {
        if (state != ClientState.READY)
            throw new ClientException("GBClient not connected");
        server.disconnect();
        this.state = ClientState.NOT_READY;
    }

    @Override
    public List<GBFile> getSharedFiles() throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

        try {
            JsonObject response = server.makeQuery("getSharedFiles", null).get().getAsJsonObject();
            return gson.fromJson(response.get("files"), new TypeToken<List<GBFile>>() {
            }.getType());
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void share (GBFile file, boolean share) throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

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
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

        JsonElement request = gson.toJsonTree(filter, GBFilter.class);
        try {
            JsonObject response = server.makeQuery("search", request).get().getAsJsonObject();
            if (response.get("success").getAsBoolean())
                throw new ClientException(response.get("error").getAsString());
            return gson.fromJson(response.get("result"), new TypeToken<List<GBFile>>() {
            }.getType());
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    /**
     * Request to the server the list of the recent files
     *
     * @param from Offset of the result list
     * @param size Limit of the result list
     * @return List of recent files
     * @throws ClientException
     */
    @Override
    public List<SyncEvent> getRecentFiles(long from, long size) throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

        // Prepare the request
        JsonObject request = new JsonObject();

        request.addProperty("from", from);
        request.addProperty("size", size);

        try {
            JsonObject response = server.makeQuery("recent", request).get().getAsJsonObject();

            // Check if there was an error
            if (response.get("success").getAsBoolean())
                throw new ClientException(response.get("error").getAsString());

            return gson.fromJson(response.get("events"), new TypeToken<List<SyncEvent>>() {}.getType());
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    /**
     * Return the list of the trashed files
     *
     * @return List of the trashed files
     * @throws ClientException
     */
    @Override
    public List<GBFile> getTrashedFiles() throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

        try {
            JsonObject response = server.makeQuery("trashed", null).get().getAsJsonObject();

            // Check if there was an error
            if (response.get("success").getAsBoolean())
                throw new ClientException(response.get("error").getAsString());

            return gson.fromJson(response.get("files"), new TypeToken<List<GBFile>>() {
            }.getType());
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException(ex.toString());
        }
    }

    @Override
    public void emptyTrash() throws ClientException {
        if (state != ClientState.READY)
            throw new IllegalStateException("client not initialized");

        try {
            JsonObject res = server.makeQuery("emptyTrash", null).get().getAsJsonObject();
            if (!res.get("success").getAsBoolean()) {
                throw new ClientException(res.get("error").getAsString());
            }
        } catch (InterruptedException ex) {
            log.warn(ex.toString(), ex);
        } catch (ExecutionException ex) {
            log.warn(ex.toString(), ex);
        }
    }

    @Override
    public void move (GBFile src, GBFile dst, boolean copy) throws ClientException {
        JsonObject req = new JsonObject();
        req.add("src", gson.toJsonTree(src, GBFile.class));
        req.add("dst", gson.toJsonTree(dst, GBFile.class));
        req.addProperty("copy", copy);
        try {
            JsonObject res = server.makeQuery("move", req).get().getAsJsonObject();
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
     * Try to switch to the selected mode. This method will block the thread.
     * If you try to switch to the current mode, nothing happen.
     *
     * @param nextMode Next mode to switch
     * @throws ClientException Switch failed
     */
    public void switchMode(ConnectionMode nextMode) throws ClientException {

        // Check if the client is connected
        if (server == null && !server.isConnected())
            throw new IllegalStateException("client not connected");

        // Assert that is not the current mode
        if (nextMode == currentTransferProfile.getMode())
            return;

        if (nextMode == ConnectionMode.BRIDGE_MODE) {
            currentTransferProfile = new TransferProfile(urls, auth);
            log.info("Switched to bridge mode");
            return;
        }

        try {

            // Ask the storage if this modality is available
            JsonObject response = server.makeQuery("directLogin", null).get().getAsJsonObject();

            // Get the ip
            String ip = nextMode == ConnectionMode.LOCAL_DIRECT_MODE ? response.get("localIP").getAsString() : response.get("publicIP").getAsString();
            String port = response.get("port").getAsString();
            String baseString = new StringBuilder("https://").append(ip).append(':').append(port).append('/').toString();

            URL baseUrl = new URL(baseString);
            URL login = new URL(baseUrl + "directLogin");

            // Get the certificate from the query response
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            byte[] encodedCertificate = gson.fromJson(response.get("certificate"), new TypeToken<byte[]>() {
            }.getType());
            InputStream inCertificate = new ByteArrayInputStream(encodedCertificate);
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inCertificate);

            // Create the new profile
            TransferProfile newProfile = new TransferProfile(urls, nextMode, baseString);

            // Create a new ssl socket factory that accepts the storage certificate
            newProfile.setSslSocketFactory(createTrustedSocketFactory(certificate));

            // Create the hostname verifier
            newProfile.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });

            // Try to call the storage to authenticate
            HttpsURLConnection conn = (HttpsURLConnection) login.openConnection();
            newProfile.prepare(conn);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Create the GBAuth object
            JsonObject json = new JsonObject();
            json.addProperty("temporaryToken", response.get("temporaryToken").getAsString());
            json.addProperty("cookie", false);

            // Send the GBAuth
            conn.getOutputStream().write(json.toString().getBytes());

            // Make the request
            if (conn.getResponseCode() != 200) {
                throw new ClientException("Switch failed.");
            }

            // Read the response
            JsonObject loginRes = new JsonParser().parse(new JsonReader(new InputStreamReader(conn.getInputStream()))).getAsJsonObject();
            newProfile.setAuthHeader("Bearer " + loginRes.get("token").getAsString());

            // Close the http connection
            conn.disconnect();

            // Switch profile
            currentTransferProfile = newProfile;
            log.info("Switched to: " + baseUrl);
        } catch (Exception ex) {
            log.warn(ex.toString(), ex);
            throw new ClientException("Switch failed");
        }
    }

    /**
     * Create a new ssl socket factory for accept the specified self signed certificate
     *
     * @param c Self signed certificate to accept
     * @return SSL Socket Factory that accepts the specified self signed certificate
     * @throws KeyStoreException        cannot instantiate keystore
     * @throws CertificateException     Invalid certificate
     * @throws NoSuchAlgorithmException
     * @throws IOException
     * @throws KeyManagementException
     */
    private static SSLSocketFactory createTrustedSocketFactory(Certificate c) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, KeyManagementException {

        // Init key store and add the certificate
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        keyStore.setCertificateEntry("GoBoxDirect", c);

        // Create a new trust manager with this keystore
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        // Create the new ssl context that accept all the certificates in the new key store
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        // Return the socket factory
        return sslContext.getSocketFactory();
    }

    /**
     * Enable or disable filter for echo sync event
     * @param echoFilter Echo filter status
     */
    public void setEchoFilter (boolean echoFilter) {
        this.filterEcho = echoFilter;
    }
}