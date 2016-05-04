package it.simonedegiacomi.goboxapi.authentication;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import it.simonedegiacomi.goboxapi.myws.MyWSClient;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Set;

/**
 * The object of this class contains the credentials of a GoBoxAccount. To use any of the API of this package you need
 * this object. Auth also provides the necessary methods to talk with the server to check the data.
 * <p>
 * Created on 31/12/15.
 *
 * @author Degiacomi Simone
 */
public class Auth {

    /**
     * Type of session: client or storage mode
     */
    public enum Modality {
        CLIENT, STORAGE
    }

    /**
     * The current mode
     */
    private Modality mode;

    /**
     * Username
     */
    private String username;

    /**
     * URLs of the environment. This is transient because it shouldn't be serialized
     */
    private transient static URLBuilder urls;

    /**
     * Token to use to authenticate with the server
     */
    private String token;

    /**
     * Set of listeners to call when the token change
     */
    private final Set<Runnable> tokenListeners = new HashSet<>();

    /**
     * Empty constructor
     */
    public Auth() {
    }

    /**
     * Let you to set the url builder that will be used to connect to the server
     *
     * @param builder Urls of the environment
     */
    public static void setUrlBuilder(URLBuilder builder) {
        urls = builder;
    }

    /**
     * Try to login with the information set. This method will block the thread until the login is complete.
     *
     * @return true if the user is logged, false if the credentials aren't valid
     * @throws IOException Network error
     */
    public boolean login(String password) throws IOException {

        // Create the json of the authentication
        JsonObject authJson = new JsonObject();
        authJson.addProperty("username", username);
        authJson.addProperty("password", password);
        authJson.addProperty("type", mode == Modality.CLIENT ? 'C' : 'S');

        // Make the https request
        HttpsURLConnection conn = (HttpsURLConnection) urls.get("login").openConnection();
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.getOutputStream().write(authJson.toString().getBytes());

        // Read the response
        if (conn.getResponseCode() != 200) {
            conn.disconnect();
            return false;
        }
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
     * Check if the token of the object is valid. This method block the thread until the response from the server is retrieved.
     *
     * @return true if the token is valid, false otherwise
     * @throws IOException Network errors
     */
    public boolean check() throws IOException {
        if (token == null)
            throw new IllegalStateException("token is null");


        HttpsURLConnection conn = (HttpsURLConnection) urls.get("authCheck").openConnection();
        authorize(conn);
        conn.setDoInput(true);

        // Read the response
        if(conn.getResponseCode() != 200) {
            conn.disconnect();
            return false;
        }
        JsonObject response = new JsonParser().parse(new JsonReader(new InputStreamReader(conn.getInputStream()))).getAsJsonObject();

        // Close the connection
        conn.disconnect();

        // If it's not 200
        if (!response.get("state").getAsString().equals("valid")) {
            return false;
        }

        // Update the token
        setToken(response.get("newOne").getAsString());

        return true;
    }

    public Modality getMode() {
        return mode;
    }

    public void setMode(Modality mode) {
        this.mode = mode;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    /**
     * Set the token and call all the listeners
     *
     * @param token New token
     */
    public void setToken(String token) {
        this.token = token;
        for (Runnable listener : tokenListeners)
            listener.run();
    }

    public String getUsername() {
        return username;
    }

    /**
     * Authorize an http connection made to the server
     *
     * @param conn Connection to authorize
     */
    public void authorize(HttpURLConnection conn) {
        conn.setRequestProperty("Authorization", getHeaderToken());
    }

    /**
     * Authorize the websocket connection
     *
     * @param server Websocket to authorize
     */
    public void authorizeWs(MyWSClient server) {
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