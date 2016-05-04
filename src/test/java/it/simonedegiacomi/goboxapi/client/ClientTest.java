package it.simonedegiacomi.goboxapi.client;

import it.simonedegiacomi.goboxapi.GBFile;
import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created on 05/04/16.
 * @author Degiacomi Simone
 */
public class ClientTest {

    private static final String USERNAME = "prova";
    private static final String PASSWORD = "prova";

    @Before
    public void init () {
        org.apache.log4j.BasicConfigurator.configure();
    }


    @Test
    public void simpleConnection () throws ClientException, IOException {

        URLBuilder urls = new URLBuilder();
        urls.load();
        Auth.setUrlBuilder(urls);
        StandardClient.setUrlBuilder(urls);

        Auth auth = new Auth();

        auth.setUsername(USERNAME);
        assertTrue(auth.login(PASSWORD));

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

    @Test
    public void directLocal () throws ClientException, IOException {
        URLBuilder urls = new URLBuilder();
        urls.load();
        Auth.setUrlBuilder(urls);
        StandardClient.setUrlBuilder(urls);

        Auth auth = new Auth();

        auth.setUsername(USERNAME);
        assertTrue(auth.login(PASSWORD));

        StandardClient client = new StandardClient(auth);
        client.onDisconnect(new StandardClient.DisconnectedListener() {
            @Override
            public void onDisconnect() {
                System.out.println("Disconnected");
            }
        });

        assertTrue(client.init());

        client.switchMode(StandardClient.ConnectionMode.LOCAL_DIRECT_MODE);


    }
}