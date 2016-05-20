package it.simonedegiacomi.goboxapi;

import org.junit.Test;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Degiacomi Simone
 * Created on 29/03/16.
 */
public class GBFileTest {

    @Test
    public void pathFromString () {
        String filePath = "/Volumes/gobox/document.pdf";
        String relativeFilePath = "document.pdf";
        String pathPrefix = "/Volumes/gobox/";

        GBFile gbFile = new GBFile();

        gbFile.setAbsolutePathByString(filePath, pathPrefix);

        assertEquals(pathPrefix, gbFile.getPrefix());
        assertEquals(relativeFilePath, gbFile.getPathAsString());
        assertEquals(filePath, gbFile.getAbsolutePathAsString());

        File file = new File(filePath);

        assertEquals(file.toString(), gbFile.toFile().toString());
    }

    @Test
    public void mixedPath () {
        String filePath = "/Volumes/gobox/music/song.mp3";
        String relativeFilePath = "music/song.mp3";
        String prefix = "/Volumes/gobox/";

        GBFile gbFile = new GBFile();
        gbFile.setAbsolutePathByString(filePath, prefix);

        assertEquals(prefix, gbFile.getPrefix());
        assertEquals(relativeFilePath, gbFile.getPathAsString());
        assertEquals(filePath, gbFile.getAbsolutePathAsString());
    }

    @Test
    public void emptyConstructor () {
        GBFile file = new GBFile();

        assertEquals(GBFile.UNKNOWN_ID, file.getID());
        assertEquals(GBFile.UNKNOWN_ID, file.getFatherID());
    }

    @Test(expected = InvalidParameterException.class)
    public void IDConstructor () {
        GBFile gbFile = new GBFile(5);
        assertEquals(5, gbFile.getID());
        assertEquals(GBFile.UNKNOWN_ID, gbFile.getFatherID());

        GBFile invalid = new GBFile(-5);
    }

    @Test
    public void constructorStringAndIsDirectory () {
        String dirName = "music";
        GBFile gbDir = new GBFile(dirName, true);

        assertEquals(dirName, gbDir.getName());
        assertTrue(gbDir.isDirectory());

        String fileName = "song.mp3";
        GBFile gbFile = new GBFile(fileName, false);
        assertEquals(fileName, gbFile.getName());
        assertFalse(gbFile.isDirectory());

        // Try with a path
        GBFile b = new GBFile("music/song.mp3", false);

        assertEquals("song.mp3", b.getName());
        assertEquals("music/song.mp3", b.getPathAsString());

        assertTrue(b.getPathAsList().get(0).isDirectory());
        assertFalse(b.getPathAsList().get(1).isDirectory());
        assertTrue(b.getPathAsList().get(1).equals(b));
    }

    @Test
    public void constructorNameFatherDir () {
        String fatherName = "music";
        GBFile gbFather = new GBFile(fatherName, true);
        gbFather.setID(23);

        GBFile gbFile = new GBFile("song.mp3", 23, false);

        assertEquals(23, gbFather.getID());
        assertEquals(gbFather.getID(), gbFile.getFatherID());
    }

    @Test
    public void constructorIdFatherNameDir () {
        GBFile gbFile = new GBFile(29, 23, "song.mp3", false);

        assertEquals("song.mp3", gbFile.getName());
        assertEquals(29, gbFile.getID());
        assertEquals(23, gbFile.getFatherID());
    }

    @Test
    public void generateChild () {
        String fatherName = "/documents/";
        GBFile father = new GBFile(fatherName, true);

        String childPath = "/documents/pdf";
        GBFile child = father.generateChild("pdf", true);

        assertEquals(childPath, child.getPathAsString());

        String fileChildPath = "/documents/pdf/file.pdf";
        GBFile fileChild = child.generateChild("file.pdf", false);

        assertEquals(fileChildPath, fileChild.getPathAsString());
    }
}