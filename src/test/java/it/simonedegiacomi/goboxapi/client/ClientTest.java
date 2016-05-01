package it.simonedegiacomi.goboxapi.client;

import com.google.gson.JsonElement;
import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.authentication.AuthException;
import it.simonedegiacomi.goboxapi.myws.WSEventListener;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import org.junit.Test;
import org.junit.internal.ExactComparisonCriteria;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created on 05/04/16.
 * @author Degiacomi Simone
 */
public class ClientTest {


    @Test
    public void simpleConnection () throws ClientException, AuthException, IOException {

        URLBuilder urls = new URLBuilder();
        urls.load();
        Auth.setUrlBuilder(urls);
        StandardClient.setUrlBuilder(urls);

        Auth auth = new Auth();

        auth.setUsername("prova");
        assertTrue(auth.login("prova"));

        StandardClient client = new StandardClient(auth);
        client.onDisconnect(new StandardClient.DisconnectedListener() {
            @Override
            public void onDisconnect() {
                System.out.println("Disconnected");
            }
        });

        assertTrue(client.init());

        GBFile fromStorage = client.getInfo(GBFile.ROOT_FILE);

        assertEquals(GBFile.ROOT_FILE, fromStorage);

    }
}
