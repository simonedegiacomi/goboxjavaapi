package it.simonedegiacomi.goboxapi.client;

import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created on 05/04/16.
 * @author Degiacomi Simone
 */
public class StandardGBClientTest {

    private static final String USERNAME = "prova";
    private static final String PASSWORD = "prova";

    @Before
    public void init () {
        org.apache.log4j.BasicConfigurator.configure();
    }


//    @Test
//    public void simpleConnection () throws ClientException, IOException {
//
//        URLBuilder urls = new URLBuilder();
//        urls.load();
//        GBAuth.setUrlBuilder(urls);
//        StandardGBClient.setUrlBuilder(urls);
//
//        GBAuth auth = new GBAuth();
//
//        auth.setUsername(USERNAME);
//        assertTrue(auth.login(PASSWORD));
//
//        StandardGBClient client = new StandardGBClient(auth);
//        client.onDisconnect(new StandardGBClient.DisconnectedListener() {
//            @Override
//            public void onDisconnect() {
//                System.out.println("Disconnected");
//            }
//        });
//
//        assertTrue(client.init());
//
//        GBFile fromStorage = client.getInfo(GBFile.ROOT_FILE);
//
//        assertEquals(GBFile.ROOT_FILE, fromStorage);
//
//    }
//
//    @Test
//    public void directLocal () throws ClientException, IOException {
//        URLBuilder urls = new URLBuilder();
//        urls.load();
//        GBAuth.setUrlBuilder(urls);
//        StandardGBClient.setUrlBuilder(urls);
//
//        GBAuth auth = new GBAuth();
//
//        auth.setUsername(USERNAME);
//        assertTrue(auth.login(PASSWORD));
//
//        StandardGBClient client = new StandardGBClient(auth);
//        client.onDisconnect(new StandardGBClient.DisconnectedListener() {
//            @Override
//            public void onDisconnect() {
//                System.out.println("Disconnected");
//            }
//        });
//
//        assertTrue(client.init());
//
//        client.switchMode(StandardGBClient.ConnectionMode.LOCAL_DIRECT_MODE);
//
//
//    }
}