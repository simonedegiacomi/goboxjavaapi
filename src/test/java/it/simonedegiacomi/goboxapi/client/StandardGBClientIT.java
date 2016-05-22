package it.simonedegiacomi.goboxapi.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import it.simonedegiacomi.MyTestUtils;
import it.simonedegiacomi.goboxapi.GBFile;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created on 05/04/16.
 * @author Degiacomi Simone
 */
public class StandardGBClientIT {

    private StandardGBClient client;
    private CountDownLatch countDownLatch;

    @Before
    public void init () throws IOException {
        org.apache.log4j.BasicConfigurator.configure();
        client = new StandardGBClient(MyTestUtils.loadAuth("client_auth.properties"));
    }

    public void stop () throws ClientException {
        client.shutdown();
    }

    @Test
    public void createFile () throws IOException, ClientException {
        countDownLatch = new CountDownLatch(1);

        // Create a test file
        File file = new File("prova.txt");
        PrintWriter toFile = new PrintWriter(file);
        toFile.println("Contenuto del file");
        toFile.close();

        // Create file in the storage
        GBFile gbFile = new GBFile(file);
        gbFile.setFatherID(GBFile.ROOT_ID);
        client.uploadFile(gbFile);

        // Check if the root now contains the file
        GBFile detailedRoot = client.getInfo(GBFile.ROOT_FILE);
        assertTrue(detailedRoot.getChildren().contains(gbFile));

        // Try to download the file
        File tempFile = new File("download.txt");
        FileOutputStream toTempFile = new FileOutputStream(tempFile);
        client.getFile(gbFile, toTempFile);
        toTempFile.close();

        assertEquals(Files.hash(file, Hashing.md5()), Files.hash(tempFile, Hashing.md5()));

        countDownLatch.countDown();
    }

    @After
    public void end () throws InterruptedException, ClientException {
        if (!countDownLatch.await(5000, TimeUnit.MILLISECONDS)) {
            stop();
            fail();
        }
    }
}