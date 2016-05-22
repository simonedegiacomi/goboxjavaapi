package it.simonedegiacomi.goboxapi.client;

import com.google.gson.annotations.Expose;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import it.simonedegiacomi.goboxapi.GBFile;

/**
 * This class is used to create the SyncEvent object that contain the information about a new event made
 * from another client onEvent the storage
 *
 * Created on 02/01/16.
 * @author Degiacomi Simone
 */
@DatabaseTable(tableName = "event")
public class SyncEvent implements Comparable {

    /**
     * Kinds of events
     */
    public enum EventKind {

        /**
         * New file created
         */
        FILE_CREATED,

        /**
         * Content of the file modified
         */
        FILE_MODIFIED,

        /**
         * File copied
         */
        FILE_COPIED,

        /**
         * File moved or renamed
         */
        FILE_MOVED,

        /**
         * File moved to trahs
         */
        FILE_TRASHED,

        /**
         * File recovered from trash
         */
        FILE_RECOVERED,

        /**
         * File deleted (from the trash or not)
         */
        FILE_DELETED,

        /**
         * File viewed, but not the preview
         */
        FILE_OPENED,

        /**
         * File shared
         */
        FILE_SHARED,

        /**
         * Stop sharing file
         */
        FILE_UNSHARED
    }

    /**
     * ID of the event
     */
    @DatabaseField(generatedId = true, canBeNull = false)
    @Expose
    private long ID;

    /**
     * Kind of this event
     */
    @DatabaseField(canBeNull = false)
    @Expose
    private EventKind kind;

    /**
     * File associated with this event.
     */
    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    @Expose
    private GBFile file;

    /**
     * If the event affected the file, this is the old configuration of the file
     */
    @Expose
    private GBFile before;

    @DatabaseField
    @Expose
    private long date;

    public SyncEvent () { }

    public SyncEvent(EventKind kind, GBFile relativeFile) {
        this.kind = kind;
        setRelativeFile(relativeFile);
    }

    public SyncEvent(EventKind kind) {
        this.kind = kind;
    }

    /**
     * Return the kind of the event as a string
     * @return string that represent the kind of the event
     */
    public String getKindAsString() { return kind.toString();}

    public EventKind getKind () { return kind; }

    public GBFile getRelativeFile(){
        return file;
    }

    /**
     * Set the file associated with this event
     * @param relativeFile File associated with this event
     */
    public void setRelativeFile(GBFile relativeFile) {
        this.file = relativeFile;
    }

    public long getID() {
        return ID;
    }

    public void setID(long ID) {
        this.ID = ID;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public GBFile getBefore () { return before; }

    public void setBefore (GBFile before) { this.before = before; }

    @Override
    public int compareTo(Object o) {
        return o == this ? 0 : 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof SyncEvent))
            return false;

        SyncEvent b = (SyncEvent) o;

        if (b.kind != kind)
            return false;
        if (ID > 0 && b.ID > 0)
            return ID == b.ID;

        // Check if the file is the same
        return file.equals(b.file);
    }
}