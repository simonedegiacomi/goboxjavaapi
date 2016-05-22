package it.simonedegiacomi.goboxapi;

import it.simonedegiacomi.IntegrationTest;
import it.simonedegiacomi.goboxapi.authentication.GBAuth;
import it.simonedegiacomi.goboxapi.authentication.PropertiesAuthLoader;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Degiacomi Simone
 * Created on 29/03/16.
 */
@Category(IntegrationTest.class)
public class GBAuthTest {

    @Test
    public void login () throws IOException {
        //GBAuth auth = PropertiesAuthLoader.loadAuth("src/test/resources/client_auth.properties");
        //assertNotNull(auth);
    }

    @Test
    public void loginWithToken () throws IOException {
//        GBAuth auth = PropertiesAuthLoader.loadAuth("src/test/resources/client_auth.properties");
//
//
//        // Re login
//        GBAuth newAuth = new GBAuth();
//        newAuth.setUsername(auth.getUsername());
//        newAuth.setMode(auth.getMode());
//        newAuth.setToken(auth.getToken());
//
//        // check the token
//        assertTrue(newAuth.check());
    }
}