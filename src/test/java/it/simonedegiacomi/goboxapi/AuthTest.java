package it.simonedegiacomi.goboxapi;

import it.simonedegiacomi.goboxapi.authentication.Auth;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Degiacomi Simone
 * Created on 29/03/16.
 */
public class AuthTest {

    private String username = "testAccount";
    private String password = "testPassword";

    @Test
    public void failAuth () throws IOException {
        Auth auth = new Auth();
        auth.setUsername("abc");
        boolean logged = auth.login("123");

        assertFalse(logged);
    }

    @Test
    public void auth () throws IOException {
        Auth auth = new Auth();
        auth.setUsername(username);
        boolean logged = auth.login(password);

        assertTrue(logged);
    }

}
