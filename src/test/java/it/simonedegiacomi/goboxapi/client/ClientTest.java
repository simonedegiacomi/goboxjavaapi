package it.simonedegiacomi.goboxapi.client;

import it.simonedegiacomi.goboxapi.authentication.Auth;
import org.junit.Test;
import org.junit.internal.ExactComparisonCriteria;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by simone on 05/04/16.
 */
public class ClientTest {

    @Test
    public void localDirectConnection () {
        try {
            Auth auth = new Auth();

            auth.setUsername("prova");
            assertTrue(auth.login("prova"));

            StandardClient client = new StandardClient(auth);

            client.connect();
            System.out.println("Connesso");
            client.switchMode(StandardClient.ConnectionMode.DIRECT_MODE);
            System.out.println("Switced");
        } catch (Exception ex) {
            fail();
        }
    }
}
