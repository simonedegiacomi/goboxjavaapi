package it.simonedegiacomi.goboxapi;

import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Degiacomi Simone
 * Created on 29/03/16.
 */
public class GBAuthTest {

    private String username = "testAccount";
    private String password = "testPassword";

    @Test
    public void failAuth () throws IOException {
        URLBuilder.DEFAULT.init();
//        GBAuth auth = new GBAuth();
//        auth.setUsername("abc");
//        boolean logged = auth.login("123");
//
//        assertFalse(logged);
    }

    @Test
    public void auth () throws IOException {
//        GBAuth auth = new GBAuth();
//        auth.setUsername(username);
//        boolean logged = auth.login(password);
//
//        assertTrue(logged);
    }

}
