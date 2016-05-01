package it.simonedegiacomi.goboxapi.client;

import com.google.gson.JsonElement;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created on 01/05/16.
 * @author Degiacomi Simone
 */
public class TransferUrlUtils {

    /**
     * Type of action
     */
    public enum Action {
        DOWNLOAD,
        UPLOAD
    }

    /**
     * Current mode of the client
     */
    private StandardClient.ConnectionMode mode;

    /**
     * Url builder
     */
    private final URLBuilder url;

    /**
     * New transfer url utilsin bridge mode
     * @param url
     */
    public TransferUrlUtils(URLBuilder url) {
        this.url = url;
        this.mode = StandardClient.ConnectionMode.BRIDGE_MODE;
    }

    /**
     * Change the current mode.
     * @param mode Current mode
     * @param base Base url, null if mode is BRIDGE_MODE
     */
    public void setMode (StandardClient.ConnectionMode mode, String base) throws MalformedURLException {
        this.mode = mode;
        if (mode != StandardClient.ConnectionMode.BRIDGE_MODE) {
            url.addUrl("receiveFileDirect", new URL(base + "fromStorage"));
            url.addUrl("uploadFileDirect", new URL(base + "toStorage"));
        }
    }

    /**
     * Get the right url to use in this mode
     * @param action Action to perform
     * @param params Params to add in the url
     * @return Right url to use
     */
    public URL getUrl (Action action, JsonElement params) {
        return getUrl(action, params, false);
    }

    /**
     *
     * Get the right url to use in this mode
     * @param action Action to perform
     * @param params Params to add in the url
     * @param single Serialize all the parameters in a single query parameter
     * @return Right url to use
     */
    public URL getUrl (Action action, JsonElement params, boolean single) {
        String key = action == Action.DOWNLOAD ? "receiveFile" : "uploadFile";
        if (mode != StandardClient.ConnectionMode.BRIDGE_MODE) {
            key += "Direct";
        }
        return url.get(key, params, single);
    }
}