package it.simonedegiacomi.goboxapi.myws;

import com.google.gson.JsonElement;
import it.simonedegiacomi.goboxapi.myws.annotations.WSEvent;
import org.junit.Test;

import java.net.URI;

public class WSTest {

    @Test
    public void handleDisconnection () {
//        try {
//            URI serverUri = new URI("wss://gobox-simonedegiacomi.c9users.io/api/ws/storage");
//            MyWSClient ws = new MyWSClient(serverUri);
//
//            ws.connect();
//
//            ws.addEventHandler(new WSEventListener() {
//                @WSEvent(name = "disconnect")
//                @Override
//                public void onEvent(JsonElement data) {
//                    System.out.println("Disconnected");
//                }
//            });
//
//            while(true) {
//                ws.sendEvent("", null);
//                Thread.sleep(685);
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }
}
