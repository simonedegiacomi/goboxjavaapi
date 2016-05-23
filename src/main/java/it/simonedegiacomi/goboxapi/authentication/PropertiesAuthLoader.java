package it.simonedegiacomi.goboxapi.authentication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Simple utilities class used to load auth object from properties
 *
 * Created on 22/05/2016
 * @author Degiacomi Simone
 */
public class PropertiesAuthLoader {

    /**
     * Load username, token and mode from properties and put them into a new auth object
     * @param properties Properties to which load the attributes
     * @return Loaded auth object
     */
    public static GBAuth loadFromProperties (Properties properties) {
        GBAuth auth = new GBAuth();
        auth.setUsername(properties.getProperty("username"));
        auth.setMode(GBAuth.Modality.valueOf(properties.getProperty("mode")));
        return auth;
    }

    /**
     * Load username, token and mode from properties and put them into a new auth object
     * @param file File to which read the properties
     * @return Loaded auth object
     * @throws IOException
     */
    public static GBAuth loadFromPropertiesFile (File file) throws IOException {
        Properties temp = new Properties();
        temp.load(new FileInputStream(file));
        return loadFromProperties(temp);
    }

    /**
     * Load username, token and mode from properties and put them into a new auth object, then try to login.
     * If the token is present, check the auth object, otherwise try to login using the password
     * @param properties Properties to which load the attributes
     * @return Auth object if logged, null otherwise
     * @throws IOException Error while authenticating
     */
    public static GBAuth loadAndLogin (Properties properties) throws IOException {
        GBAuth auth = loadFromProperties(properties);
        if (auth.getToken() != null) {
            return auth.check() ? auth : null;
        }
        return auth.login(properties.getProperty("password")) ? auth : null;
    }

    /**
     * Load username, token and mode from properties and put them into a new auth object, then try to login.
     * If the token is present, check the auth object, otherwise try to login using the password
     * @param file File to which read the properties
     * @return Auth object if logged, null otherwise
     * @throws IOException Error while authenticating
     */
    public static GBAuth loadAndLoginFromFile (File file) throws IOException {
        Properties temp = new Properties();
        temp.load(new FileInputStream(file));
        return loadAndLogin(temp);
    }
}