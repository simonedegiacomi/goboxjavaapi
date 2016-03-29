package it.simonedegiacomi.goboxapi;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * @author Degiacomi Simone
 * Created on 29/03/16.
 */
public class GBFileTest {

    @Test
    public void pathTest () {
        String filePath = "/Volumes/gobox/document.pdf";
        String relativeFilePath = "document.pdf";
        File file = new File(filePath);
        String pathPrefix = "/Volumes/gobox/";

        GBFile gbFile = new GBFile(file, pathPrefix);

        assertEquals(file.getName(), gbFile.getName());
        assertEquals(file.toString(), gbFile.getAbsolutePathAsString());
        assertEquals(pathPrefix, gbFile.getPrefix());

        gbFile = new GBFile();

        gbFile.setAbsolutePathByString(filePath, pathPrefix);

        assertEquals(pathPrefix, gbFile.getPrefix());
        assertEquals(relativeFilePath, gbFile.getPathAsString());
        assertEquals(filePath, gbFile.getAbsolutePathAsString());

        assertEquals(file.toString(), gbFile.toFile().toString());

    }
}
