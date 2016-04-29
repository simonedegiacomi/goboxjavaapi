package it.simonedegiacomi.goboxapi;

import java.util.HashMap;

/**
 * Created on 18/02/16.
 * @author Degiacomi Simone
 */
public class GBCache {
    private final HashMap<Long, GBFile> cacheById = new HashMap<>();
    private final HashMap<String, GBFile> cacheByPath = new HashMap<>();

    public void add (GBFile file) {
        if(file.getID() != GBFile.UNKNOWN_ID)
            cacheById.put(file.getID(), file);
        cacheByPath.put(file.getPathAsString(), file);
        if(file.getChildren() == null)
            return;
        for(GBFile child : file.getChildren())
            if(!child.isDirectory())
                add(child);
    }

    public GBFile get (long id) {
        return cacheById.get(id);
    }

    public GBFile get (String path) {
        return cacheByPath.get(path);
    }

    public GBFile get (GBFile poorFile) {
        if(poorFile.getID() != GBFile.UNKNOWN_ID && cacheById.containsKey(poorFile.getID()))
            return cacheById.get(poorFile.getID());
        if (poorFile.getPathAsList() != null)
            return cacheByPath.get(poorFile.getPathAsString());
        return null;
    }
}
