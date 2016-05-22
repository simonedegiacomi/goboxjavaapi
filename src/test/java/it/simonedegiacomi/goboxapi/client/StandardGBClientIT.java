package it.simonedegiacomi.goboxapi.client;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import it.simonedegiacomi.IntegrationTest;
import it.simonedegiacomi.goboxapi.authentication.PropertiesAuthLoader;
import it.simonedegiacomi.goboxapi.GBFile;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created on 05/04/16.
 * @author Degiacomi Simone
 */
@Category(IntegrationTest.class)
public class StandardGBClientIT {

    private final Logger log = Logger.getLogger(StandardGBClientIT.class);

    private StandardGBClient client;
    private CountDownLatch countDownLatch;

    @Before
    public void init () throws IOException, ClientException {
        org.apache.log4j.BasicConfigurator.configure();
        client = new StandardGBClient(PropertiesAuthLoader.loadAndLoginFromFile(new File("client_auth.properties")));
        client.init();
    }

    public void stop () throws ClientException {
        client.shutdown();
    }

    @Test
    public void createFile () throws IOException, ClientException {
        countDownLatch = new CountDownLatch(2);

        // Prepare snc listener
        SyncEvent expectedEvent = new SyncEvent(SyncEvent.EventKind.FILE_CREATED);
        client.addSyncEventListener(new SyncEventListener() {

            @Override
            public void on(SyncEvent event) {
                assertEquals(expectedEvent, event);
                countDownLatch.countDown();
            }
        });

        // Create a test file
        File file = new File("prova.txt");
        PrintWriter toFile = new PrintWriter(file);
        toFile.println("Contenuto del file");
        toFile.close();

        // Create file in the storage
        GBFile gbFile = new GBFile(file);
        expectedEvent.setRelativeFile(gbFile);
        gbFile.setFatherID(GBFile.ROOT_ID);
        client.uploadFile(gbFile);
        log.info("File uploaded");

        // Check if the root now contains the file
        GBFile detailedRoot = client.getInfo(GBFile.ROOT_FILE);
        assertTrue(detailedRoot.getChildren().contains(gbFile));
        log.info("File exists");

        // Try to download the file
        File tempFile = new File("download.txt");
        FileOutputStream toTempFile = new FileOutputStream(tempFile);
        client.getFile(detailedRoot.getChildren().get(detailedRoot.getChildren().indexOf(gbFile)), toTempFile);
        toTempFile.close();
        log.info("File downloaded");

        assertEquals(Files.hash(file, Hashing.md5()), Files.hash(tempFile, Hashing.md5()));

        countDownLatch.countDown();
    }

    @Test
    public void createFolder () throws ClientException {
        countDownLatch = new CountDownLatch(2);

        // Prepare sync event listener
        SyncEvent expectedEvent = new SyncEvent(SyncEvent.EventKind.FILE_CREATED);
        client.addSyncEventListener(new SyncEventListener() {
            @Override
            public void on(SyncEvent event) {
                assertEquals(expectedEvent, event);
                countDownLatch.countDown();
            }
        });

        // Create a test directory
        GBFile folder = new GBFile("directory" + new Random().nextInt(), GBFile.ROOT_ID, true);
        expectedEvent.setRelativeFile(folder);
        client.createDirectory(folder);
        log.info("NEw folder created");

        // Check if exists
        GBFile detailedRoot = client.getInfo(GBFile.ROOT_FILE);
        assertTrue(detailedRoot.getChildren().contains(folder));

        countDownLatch.countDown();
    }

    @Test
    public void createAndDeleteFolder () throws ClientException {
        countDownLatch = new CountDownLatch(3);

        // Prepare sync event listener
        SyncEvent expectedCreation = new SyncEvent(SyncEvent.EventKind.FILE_CREATED);
        SyncEvent expectedDeletion = new SyncEvent(SyncEvent.EventKind.FILE_DELETED);
        client.addSyncEventListener(new SyncEventListener() {
            @Override
            public void on(SyncEvent event) {
                if (event.getKind() == SyncEvent.EventKind.FILE_CREATED) {
                    assertEquals(expectedCreation, event);
                    countDownLatch.countDown();
                    return;
                }
                assertEquals(expectedDeletion, event);
                countDownLatch.countDown();
            }
        });

        // Create a folder
        GBFile newFolder = new GBFile("toDelete", GBFile.ROOT_ID, true);
        expectedCreation.setRelativeFile(newFolder);
        expectedDeletion.setRelativeFile(newFolder);
        client.createDirectory(newFolder);
        log.info("Folder created");

        // Delete it
        client.removeFile(newFolder);
        log.info("Folder deleted");

        countDownLatch.countDown();
    }

    @Test
    public void trashRecoverAndRemoveFile () throws ClientException, IOException {
        countDownLatch = new CountDownLatch(5);

        // Prepare sync event listener
        SyncEvent expectedCreation = new SyncEvent(SyncEvent.EventKind.FILE_CREATED);
        SyncEvent expectedTrash = new SyncEvent(SyncEvent.EventKind.FILE_TRASHED);
        SyncEvent expectedRecover = new SyncEvent(SyncEvent.EventKind.FILE_RECOVERED);
        SyncEvent expectedDeletion = new SyncEvent(SyncEvent.EventKind.FILE_DELETED);
        client.addSyncEventListener(new SyncEventListener() {
            @Override
            public void on(SyncEvent event) {
                if (event.getKind() == SyncEvent.EventKind.FILE_CREATED) {
                    assertEquals(expectedCreation, event);
                    countDownLatch.countDown();
                    return;
                }

                if (event.getKind() == SyncEvent.EventKind.FILE_TRASHED) {
                    assertEquals(expectedTrash, event);
                    countDownLatch.countDown();
                    return;
                }

                if (event.getKind() == SyncEvent.EventKind.FILE_RECOVERED) {
                    assertEquals(expectedRecover, event);
                    countDownLatch.countDown();
                    return;
                }

                assertEquals(expectedDeletion, event);
                countDownLatch.countDown();
            }
        });

        // Create a test file
        File file = new File("prova.txt");
        PrintWriter toFile = new PrintWriter(file);
        toFile.println("Contenuto del file");
        toFile.close();

        // Upload it
        GBFile newFile = new GBFile(file);
        expectedCreation.setRelativeFile(newFile);
        expectedTrash.setRelativeFile(newFile);
        expectedRecover.setRelativeFile(newFile);
        expectedDeletion.setRelativeFile(newFile);
        client.uploadFile(newFile);
        log.info("File uploaded");

        // Trash file
        client.trashFile(newFile, true);
        log.info("File trashed");

        // Recover file
        client.trashFile(newFile, false);
        log.info("File recovered");

        // Delete the file
        client.removeFile(newFile);
        log.info("File removed");
    }

    public void move (boolean copy) throws IOException, ClientException {

        countDownLatch = new CountDownLatch(2);

        // Prepare sync event listener
        SyncEvent expectedCreation = new SyncEvent(SyncEvent.EventKind.FILE_CREATED);
        SyncEvent expectedMotion = new SyncEvent(copy ? SyncEvent.EventKind.FILE_COPIED : SyncEvent.EventKind.FILE_MOVED);
        client.addSyncEventListener(new SyncEventListener() {
            @Override
            public void on(SyncEvent event) {
                if (expectedCreation.getKind() == SyncEvent.EventKind.FILE_CREATED) {
                    assertEquals(expectedCreation, event);
                    countDownLatch.countDown();
                }
            }
        });

        // Create a new file
        File file = new File("prova" + new Random().nextInt() +".txt");
        PrintWriter toFile = new PrintWriter(file);
        toFile.println("Contenuto del file");
        toFile.close();

        // Upload it
        GBFile fileToUpload = new GBFile(file);
        expectedCreation.setRelativeFile(fileToUpload);
        fileToUpload.setFatherID(GBFile.ROOT_ID);
        client.uploadFile(fileToUpload);
        log.info("File uploaded");

        // Get the file
        GBFile detailedRoot = client.getInfo(GBFile.ROOT_FILE);
        GBFile detailedUploaded = detailedRoot.getChildren().get(detailedRoot.getChildren().indexOf(fileToUpload));
        log.info("File exists");

        // Change name
        client.move(detailedUploaded, new GBFile("moved.txt", GBFile.ROOT_ID, false), copy);
        log.info("File moved");
    }

    @Test
    public void moveFile () throws IOException, ClientException {
        move(false);
    }

    @Test
    public void copy () throws IOException, ClientException {
        move(true);
    }

    @After
    public void end () throws InterruptedException, ClientException {
        if (!countDownLatch.await(5000, TimeUnit.MILLISECONDS)) {
            stop();
            fail();
        }
    }
}