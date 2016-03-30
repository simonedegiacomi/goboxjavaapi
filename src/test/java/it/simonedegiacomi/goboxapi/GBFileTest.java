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
    public void pathFromJavaFile () {
        String filePath = "/Volumes/gobox/document.pdf";
        File file = new File(filePath);
        String pathPrefix = "/Volumes/gobox/";

        GBFile gbFile = new GBFile(file, pathPrefix);

        assertEquals(file.getName(), gbFile.getName());
        assertEquals(file.toString(), gbFile.getAbsolutePathAsString());
        assertEquals(pathPrefix, gbFile.getPrefix());
    }

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

        GBFile invalid = new GBFile(-1);
    }

    @Test
    public void constructorStringAndIsDirectoryConstructor () {
        String dirName = "music";
        GBFile gbDir = new GBFile(dirName, true);

        assertEquals(dirName, gbDir.getName());
        assertTrue(gbDir.isDirectory());

        String fileName = "song.mp3";
        GBFile gbFile = new GBFile(fileName, false);
        assertEquals(fileName, gbFile.getName());
        assertFalse(gbFile.isDirectory());
    }

    @Test
    public void constructorFile () {
        File file = new File("school/documents/math.doc");
        GBFile gbFile = new GBFile(file);

        // Test the string path
        assertEquals(file.toString(), gbFile.getPathAsString());

        // Test the list path
        List<GBFile> correct = new LinkedList<>();
        correct.add(new GBFile("school", true));
        correct.add(new GBFile("documents", true));
        correct.add(new GBFile("math.doc", false));

        List<GBFile> generated = gbFile.getAbsolutePathAsList();

        assertTrue(correct.equals(generated));
    }

    @Test
    public void constructorFileAndPrefix () {
        String prefix = "/gobox/";
        File file = new File("/gobox/school/documents/math.doc");
        GBFile gbFile = new GBFile(file, prefix);

        assertEquals(gbFile.getPrefix(), prefix);
        assertEquals("school/documents/math.doc", gbFile.getPathAsString());

        // Test the absolute path as list
        List<GBFile> correct = new LinkedList<>();
        correct.add(new GBFile("", true)); // The root
        correct.add(new GBFile("gobox", true));
        correct.add(new GBFile("school", true));
        correct.add(new GBFile("documents", true));
        correct.add(new GBFile("math.doc", false));

        List<GBFile> generated = gbFile.getAbsolutePathAsList();

        assertTrue(correct.equals(generated));
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
    public void equal () {
        String prefix = "/gobox/";
        File f = new File ("/gobox/files/myFile.txt");

        GBFile a = new GBFile(f, prefix);
        GBFile b = new GBFile(f, prefix);

        assertTrue(a.equals(b));

        GBFile c = new GBFile(f);
        assertTrue(a.equals(c));
    }
}