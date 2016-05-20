package it.simonedegiacomi.goboxapi.client;

import com.google.gson.JsonObject;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;

/**
 * This class helps to manage the urls to download and upload file from/to the storage.
 * Created on 01/05/16.
 * @author Degiacomi Simone
 */
public class TransferProfile {

    /**
     * Type of action
     */
    public enum Action {
        DOWNLOAD,
        UPLOAD
    }

    /**
     * Mode of this profile
     */
    private final StandardGBClient.ConnectionMode mode;

    /**
     * GBAuth header for this profile
     */
    private String authHeader;

    /**
     * SSl socket factory
     */
    private SSLSocketFactory sslSocketFactory;

    /**
     * Host name verifier
     */
    private HostnameVerifier hostnameVerifier;

    /**
     * Url builder
     */
    private final URLBuilder urls;

    /**
     * Create a new profile in bridge mode
     * @param url Url builder to use
     * @param GBAuth GBAuth object with the user credentials
     */
    public TransferProfile (URLBuilder url, GBAuth GBAuth) {
        this.urls = url;
        mode = StandardGBClient.ConnectionMode.BRIDGE_MODE;
        authHeader = "Bearer " + GBAuth.getToken();
    }

    /**
     * Create anew profile in non-bridge mode.
     * @param url Url builder to use
     * @param mode Current mode
     * @param base Base url for the current mode
     * @throws MalformedURLException
     */
    public TransferProfile (URLBuilder url, StandardGBClient.ConnectionMode mode, String base) throws MalformedURLException {
        if (mode == StandardGBClient.ConnectionMode.BRIDGE_MODE)
            throw new InvalidParameterException("invalid mode");

        this.urls = url;
        this.mode = mode;
        url.addUrl("receiveFile" + mode, new URL(base + "fromStorage"));
        url.addUrl("uploadFile" + mode, new URL(base + "toStorage"));
    }

    /**
     * Set the auth header. This method is meaningless in bridge mode
     * @param authHeader GBAuth header
     */
    public void setAuthHeader (String authHeader) {
        if (mode == StandardGBClient.ConnectionMode.BRIDGE_MODE)
            throw new IllegalStateException("bridge mode don't need auth header");
        this.authHeader = authHeader;
    }

    public void setSslSocketFactory (SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public void setHostnameVerifier (HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public StandardGBClient.ConnectionMode getMode() {
        return mode;
    }

    /**
     * Prepare the specified connection with the current profile
     * @param conn Connection to prepare
     */
    public void prepare (HttpsURLConnection conn) {

        // Add authorization header
        if (authHeader != null) {
            conn.addRequestProperty("Authorization", authHeader);
        }

        // Add the ssl socket factory
        if (sslSocketFactory != null) {
            conn.setSSLSocketFactory(sslSocketFactory);
        }

        // Add the hostname verifier
        if (hostnameVerifier != null) {
            conn.setHostnameVerifier(hostnameVerifier);
        }
    }

    /**
     * Get the right urls to use in this mode
     * @param action Action to perform
     * @param params Params to add in the urls
     * @return Right urls to use
     */
    public URL getUrl (Action action, JsonObject params) {
        return getUrl(action, params, false);
    }

    /**
     *
     * Get the right urls to use in this mode
     * @param action Action to perform
     * @param params Params to add in the urls
     * @param single Serialize all the parameters in a single query parameter
     * @return Right urls to use
     */
    public URL getUrl (Action action, JsonObject params, boolean single) {
        String key = action == Action.DOWNLOAD ? "receiveFile" : "uploadFile";
        if (mode != StandardGBClient.ConnectionMode.BRIDGE_MODE) {
            key += mode;
        }
        return urls.get(key, params, single);
    }

    /**
     * Use the {@link #getUrl(Action, JsonObject, boolean)}, open the connection and prepare it
     * @param action Action
     * @param params Parameters
     * @param single Single parameter
     * @return Prepared connection
     * @throws IOException
     */
    public HttpsURLConnection openConnection (Action action, JsonObject params, boolean single) throws IOException {
        HttpsURLConnection conn = (HttpsURLConnection) getUrl(action, params, single).openConnection();
        prepare(conn);
        return conn;
    }
}