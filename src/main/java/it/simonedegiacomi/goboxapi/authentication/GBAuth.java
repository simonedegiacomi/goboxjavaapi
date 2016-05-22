package it.simonedegiacomi.goboxapi.authentication;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import com.google.gson.stream.JsonReader;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.utils.MyGsonBuilder;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;

/**
 * The object of this class contains the credentials of a GoBoxAccount. To use any of the API of this package you need
 * this object. GBAuth also provides the necessary methods to talk with the server to check the data.
 *
 * Created on 31/12/15.
 *
 * @author Degiacomi Simone
 */
public class GBAuth {

    /**
     * Type of session: client or storage mode
     */
    public enum Modality {

        /**
         * Mode to authenticate as a client
         */
        CLIENT,

        /**
         * Mode to authenticate as a storage
         */
        STORAGE
    }

    /**
     * URLs of the environment.
     */
    private static final URLBuilder urls = URLBuilder.DEFAULT;

    /**
     * Gson used to serialize the instance of this object in json
     */
    private final Gson gson = MyGsonBuilder.create();

    /**
     * The current mode
     */
    @Expose
    private Modality mode;

    /**
     * Username
     */
    @Expose
    private String username;

    /**
     * Token to use to authenticate with the server
     */
    @Expose
    private String token;

    /**
     * Set of listeners to call when the token changes
     */
    private final Set<Runnable> tokenListeners = new HashSet<>();

    /**
     * Empty constructor for gson
     */
    public GBAuth() {}

    /**
     * Create a new auth token given the username and the last used token
     * @param username Username
     * @param token Token
     */
    public GBAuth(String username, String token) {
        this.username = username;
        this.token = token;
    }

    /**
     * Try to login with the information set. This method will block the thread until the login is complete.
     * @param password Password of the account
     * @return true if the user is logged, false if the credentials aren't valid
     * @throws IOException Network error
     */
    public boolean login (String password) throws IOException {

        // Serialize this object to json
        JsonObject authJson = gson.toJsonTree(this, GBAuth.class).getAsJsonObject();

        // Add password field
        authJson.addProperty("password", password);

        // Make the https request
        HttpsURLConnection conn = (HttpsURLConnection) urls.get("login").openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.getOutputStream().write(authJson.toString().getBytes());

        // Check the response code
        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            return false;
        }

        // Read the response
        JsonObject response = new JsonParser().parse(new JsonReader(new InputStreamReader(conn.getInputStream()))).getAsJsonObject();

        // close the connection
        conn.disconnect();

        if (!response.get("result").getAsString().equals("logged in")) {
            return false;
        }

        // Set the token
        setToken(response.get("token").getAsString());
        return true;
    }

    /**
     * Check if the token of the object is valid. This method block the thread until the response from the server is
     * retrieved.
     *
     * @return true if the token is valid, false otherwise
     * @throws IOException Network errors
     */
    public boolean check () throws IOException {

        // Prepare the connection
        HttpsURLConnection conn = (HttpsURLConnection) urls.get("authCheck").openConnection();
        authorize(conn);
        conn.setDoInput(true);

        // Read the response code
        if(conn.getResponseCode() != 200) {
            conn.disconnect();
            return false;
        }

        JsonObject response = new JsonParser().parse(new JsonReader(new InputStreamReader(conn.getInputStream()))).getAsJsonObject();

        // Close the connection
        conn.disconnect();

        if (!response.get("state").getAsString().equals("valid")) {
            return false;
        }

        // Update the token
        setToken(response.get("newOne").getAsString());

        return true;
    }

    /**
     * Return the current mode of this auth
     * @return current mode
     */
    public Modality getMode() {
        return mode;
    }

    /**
     * Set the mode of this auth object.
     * @param mode Current mode
     */
    public void setMode(Modality mode) {
        this.mode = mode;
    }

    /**
     * Set the username for this auth object
     * @param username Username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Return the current auth token
     * @return Authentication token
     */
    public String getToken() {
        return token;
    }

    public void setToken (String newToken) {
        this.token = newToken;
        for (Runnable listener : tokenListeners) {
            listener.run();
        }
    }

    /**
     * Return the username of this auth object
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Authorize an http connection made to the server
     *
     * @param conn Connection to authorize
     */
    public void authorize (HttpURLConnection conn) {
        conn.setRequestProperty("Authorization", getHeaderToken());
    }

    /**
     * Authorize the websocket connection
     *
     * @param server Websocket to authorize
     */
    public void authorize (MyWSClient server) {
        server.addHttpHeader("Authorization", getHeaderToken());
    }

    /**
     * Return the value of the 'Authorization' http header
     *
     * @return Value of the authorization http header
     */
    private String getHeaderToken() {
        return "Bearer " + token;
    }

    /**
     * Add a new listener that will be called when the auth token change. You can you this to update your
     * account information
     *
     * @param listener Listener to call when the token change
     */
    public void addOnTokenChangeListener(Runnable listener) {
        tokenListeners.add(listener);
    }
}