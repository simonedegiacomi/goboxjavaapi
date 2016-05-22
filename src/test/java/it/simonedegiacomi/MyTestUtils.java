package it.simonedegiacomi;

import it.simonedegiacomi.goboxapi.authentication.GBAuth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class MyTestUtils {

    public static GBAuth loadAuth(String fileName) throws IOException {
        //Properties properties = new Properties();
        //properties.load(MyTestUtils.class.getResourceAsStream(fileName));
        GBAuth auth = new GBAuth();
        //auth.setMode(GBAuth.Modality.valueOf(properties.getProperty("mode")));
        //auth.setUsername(properties.getProperty("username"));
        //return auth.login(properties.getProperty("password")) ? auth : null;
        auth.setUsername("prova");
        auth.setMode(fileName.contains("storage") ? GBAuth.Modality.STORAGE : GBAuth.Modality.CLIENT);
        return auth.login("prova") ? auth : null;
    }
}
