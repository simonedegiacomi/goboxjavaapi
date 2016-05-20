package it.simonedegiacomi.goboxapi;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Representation of a file sharing
 * Created on 15/02/16.
 * @author Degiacomi simone
 */
@DatabaseTable(tableName = "sharing")
public class Sharing {

    /**
     * ID of the sharing
     */
    @DatabaseField(columnName = "ID", generatedId = true, canBeNull = false)
    @Expose
    private long id;

    /**
     * Reference to the shared file
     */
    @DatabaseField(foreign = true)
    @Expose
    private GBFile file;

    public Sharing () { }

    public Sharing (GBFile file) {
        this.setFile(file);
    }

    /**
     * Return the id of the sharing
     * @return ID of the sharing
     */
    public long getId() {
        return id;
    }

    /**
     * Set the id of the sharing
     * @param id id of the sharing
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Return the shared file
     * @return Shared file
     */
    public GBFile getFile() {
        return file;
    }

    /**
     * Set the shared file
     * @param file shared file
     */
    public void setFile(GBFile file) {
        this.file = file;
    }
}