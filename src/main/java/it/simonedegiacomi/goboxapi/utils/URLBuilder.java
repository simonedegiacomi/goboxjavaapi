package it.simonedegiacomi.goboxapi.utils;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * This object is used to store the url to use in the program.
 * It also can create new url appending parameters to the url.
 *
 * Created on 09/01/16.
 * @author Degiacomi Simone
 */
public class URLBuilder {

    /**
     * Name of the file with default url
     */
    private static final String DEFAULT_URLS_LOCATION = "/urls.properties";

    /**
     * Properties that contains the url
     */
    private final Properties properties = new Properties();

    /**
     * Load the urls from the specified input stream
     * @param in Stream to which read the properties
     * @throws IOException
     */
    public void load (InputStream in) throws IOException {
        properties.load(in);
    }

    /**
     * Load the url builder with the default url
     * @throws IOException
     */
    public void load () throws IOException {
        load(URLBuilder.class.getResourceAsStream(DEFAULT_URLS_LOCATION));
    }

    /**
     * Return a new url specifying the key
     * @param what The key (name of the url)
     * @return Corresponding url. If the url doesn't
     * exist a null pointer will be returned
     */
    public URL get (String what) {
        try {
            return new URL(properties.getProperty(what));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    /**
     * Return the specified url in uri
     * @param what Name of the url
     * @return Uri
     */
    public URI getURI (String what) {
        try {
            return new URI(properties.getProperty(what));
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    /**
     * Return the specified url in string
     * @param what Name of the url
     * @return Url in string
     */
    public String getAsString (String what) {
        return get(what).toString();
    }

    /**
     * Return a new url with the specified parameters serialized in the url as query parameters.
     * This method is an alias for {@link #get(String, JsonObject, boolean)}
     * @param what Key of the url
     * @param params Parameters to append to the url
     * @return URL with the specified parameters serialized in query parameters.
     */
    public URL get (String what, JsonObject params) {
        try {
            return get(what, params, false);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Return a new url with the specified parameters serialized in query parameters
     *
     * Example: http://domain.com?json={"key"="value"}
     * @param what Key of the url
     * @param params Parameters to serialize
     * @param singleParam If true all the json will be serialized in only a single query string filed named 'json'
     * @return Url with the specified parameters
     */
    public URL get (String what, JsonObject params, boolean singleParam) {
        try {
            return URLParams.createURL(properties.get(what).toString(), (JsonObject) params, singleParam);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Add a new url to the internal map
     * @param key Name of the url
     * @param url Url
     */
    public void addUrl (String key, URL url) {
        properties.put(key, url);
    }

    /**
     * Remove the specified url from the internal map
     * @param key Url to remove
     */
    public void removeUrl (String key) {
        properties.remove(key);
    }
}