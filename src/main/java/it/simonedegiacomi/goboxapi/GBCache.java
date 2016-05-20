package it.simonedegiacomi.goboxapi;

import java.util.HashMap;

/**
 * Class used to provide a simple cache function for the gobox client api
 * Created on 18/02/16.
 * @author Degiacomi Simone
 */
public class GBCache {

    /**
     * Map by id
     */
    private final HashMap<Long, GBFile> cacheById = new HashMap<>();

    /**
     * Map by path
     */
    private final HashMap<String, GBFile> cacheByPath = new HashMap<>();

    /**
     * Add the file to the cache
     * @param file File to cache
     */
    public void add (GBFile file) {

        // Add the file in the id map
        if(file.getID() != GBFile.UNKNOWN_ID)
            cacheById.put(file.getID(), file);

        // add the file in the cache path
        cacheByPath.put(file.getPathAsString(), file);
        if(file.getChildren() == null)
            return;

        // Cache the children file
        if (file.isDirectory() && file.getChildren() != null) {
            for (GBFile child : file.getChildren()) {
                if (!child.isDirectory()) {
                    add(child);
                }
            }
        }
    }

    /**
     * Get cached file by id
     * @param id Id of the file
     * @return Cached file or null
     */
    public GBFile get (long id) {
        return cacheById.get(id);
    }

    /**
     * Get cached file by path
     * @param path path of the file
     * @return cached file or null
     */
    public GBFile get (String path) {
        return cacheByPath.get(path);
    }

    /**
     * Return the cached file
     * @param poorFile Poor file
     * @return Cached file or null
     */
    public GBFile get (GBFile poorFile) {

        // Check the id
        if(poorFile.getID() != GBFile.UNKNOWN_ID && cacheById.containsKey(poorFile.getID()))
            return cacheById.get(poorFile.getID());

        // check the path
        if (poorFile.getPathAsList() != null && cacheByPath.containsKey(poorFile.getPathAsString()))
            return cacheByPath.get(poorFile.getPathAsString());

        return null;
    }

    /**
     * Invalidate the cache of the specified file
     * @param file File to which invalidate the cached value
     */
    public void invalidate (GBFile file) {
        if (file.getID() != GBFile.UNKNOWN_ID) {
            cacheById.remove(file.getID());
        }

        if (file.getPathAsList() != null) {
            cacheByPath.remove(file.getPathAsString());
        }
    }
}