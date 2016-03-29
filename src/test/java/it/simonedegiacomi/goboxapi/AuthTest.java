package it.simonedegiacomi.goboxapi;

import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.authentication.AuthException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Degiacomi Simone
 * Created on 29/03/16.
 */
public class AuthTest {

    private String username = "prova";
    private String password = "prova";

    @Test
    public void failAuth () throws AuthException {
        Auth auth = new Auth();
        auth.setUsername("abc");
        boolean logged = auth.login("123");

        assertFalse(logged);
    }

    @Test
    public void auth () throws AuthException {
        Auth auth = new Auth();
        auth.setUsername(username);
        boolean logged = auth.login(password);

        assertTrue(logged);
    }

}
