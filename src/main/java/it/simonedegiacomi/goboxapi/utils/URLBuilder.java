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
 * This object is used to store the urls to use by the client and other GoBox API classes.
 *
 * Created on 09/01/16.
 * @author Degiacomi Simone
 */
public class URLBuilder {

    /**
     * Default empty url builder
     */
    public static final URLBuilder DEFAULT = new URLBuilder();

    /**
     * Name of the default file in the resources folder
     */
    public static final String DEFAULT_URLS_LOCATION = "/urls.properties";

    /**
     * Properties that contains the urls
     */
    private final Properties properties = new Properties();

    /**
     * Load the urls from the specified input stream
     * @param in Stream to which read the properties
     * @throws IOException  Exception while loading the urls
     */
    public void load (InputStream in) throws IOException {
        properties.load(in);
    }

    /**
     * Load the url builder with the default file in the resources folder
     * @throws IOException Exception while loading the urls
     */
    public void init () throws IOException {
        load(URLBuilder.class.getResourceAsStream(DEFAULT_URLS_LOCATION));
    }

    /**
     * Return a new url specifying the key
     * @param what The key (alias of the url)
     * @return Corresponding url. If the url doesn't exist a null pointer will be returned
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
     * Return a new url with the specified parameters serialized in the url as query parameters.
     * this method work only for string json property.
     * This method is an alias for {@link #get(String, JsonObject, boolean)}, called with false as last parameter
     * @param what Alias of the url
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
     * @param what Alias of the url
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
     * @param key Alias of the url
     * @param url Url
     */
    public void addUrl (String key, URL url) {
        properties.put(key, url);
    }

    /**
     * Remove the specified url from the internal map
     * @param key Alias of the url to remove
     */
    public void removeUrl (String key) {
        properties.remove(key);
    }
}