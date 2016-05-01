package it.simonedegiacomi.goboxapi.client;

import it.simonedegiacomi.goboxapi.GBFile;

import java.io.*;
import java.util.List;

/**
 * This is the interface of the goboxclient api and define the basic operation
 * that a client can do.
 *
 * @author Degiacomi Simone
 * Created on 02/01/2016.
 */
public abstract class Client {

    /**
     * Possible state of the client
     */
    public enum ClientState { INIT, READY, NOT_READY };

    /**
     * Check if the client is ready to perform operations. The difference between the {@link #getState()}
     * method is that if the client is in init phase, this will return false, because the client is
     * not ready.
     * @return Connected to the server or not
     */
    public abstract boolean isReady ();

    /**
     * Return the current state of the client.
     * @return
     */
    public abstract ClientState getState ();

    /**
     * Init the client
     * @return True if the client in now ready
     * @throws ClientException
     */
    public abstract boolean init () throws ClientException;

    /**
     * Close the connection with the storage and release all the resources.
     */
    public abstract void shutdown () throws ClientException;

    /**
     * Return a new GBFile with the info retrieved from the storage. If the file is not found
     * a null pointer will be returned. The exception is thrown if there's some connection error.
     *
     * When the file is missing i return a null pointer instead throwing a new exception because
     * you can call this method to check if a file exist, so a null pointer can be a logical value
     * (exist or not).
     * @param file File to look at
     * @return GBFile with the storage information
     * @throws ClientException Exception if there some problem with the communication to
     * the storage
     */
    public abstract GBFile getInfo(GBFile file) throws ClientException;

    /**
     * Retrieve the file from the storage and save it to the file position saved inside the GBFile
     * passed as argument. If the file doesn't exist a new exception is thrown. Int his case an exception
     * because you're supposing that the file exist
     * @param file File to retrieve.
     * @throws ClientException Exception thrown in case of invalid id, network error or io error while saving the
     * file to the disk
     */
    public void getFile (GBFile file) throws ClientException, IOException {
        getFile(file, new FileOutputStream(file.toFile()));
    }

    /**
     * Same as getFile(GBFile) but let you specify the output stream that will be used to write the incoming file
     * from the storage
     * @param file File to download
     * @param dst Destination of the input stream of the file
     * @throws ClientException Exception thrown in case of
     * invalid id or network error
     */
    public abstract void getFile (GBFile file, OutputStream dst) throws ClientException, IOException;

    /**
     * Create a new directory
     * @param newDir Directory to create
     * @throws ClientException thrown if a folder with the same name exist or other reason of the storage
     */
    public abstract void createDirectory (GBFile newDir) throws ClientException;

    /**
     * Send a file to the storage.
     * @param file File to send File to send. The object must have or the field father id or the path.
     * @param stream Stream of the file Stream that will be sent to the storage
     * @throws ClientException Exception Network error or invalid father reference
     */
    public abstract void uploadFile (GBFile file, InputStream stream) throws ClientException, IOException;

    /**
     * Same ad uploadFile(GBFile, InputStream) but this read the file from the path of the GBFile
     * @param file File to send
     * @throws ClientException Exception Network error, null file or invalid
     * father reference
     */
    public void uploadFile (GBFile file) throws ClientException, IOException {
        uploadFile(file, new FileInputStream(file.toFile()));
    }

    /**
     * Move a file from/to the trash
     * @param file File to move
     * @param toTrash True to move the file in the trash, false otherwise
     * @throws ClientException
     */
    public abstract void trashFile (GBFile file, boolean toTrash) throws ClientException;

    /**
     * Move a file to/from the trash using {@link #isTrashed} method. This method is an alias for
     * {@link #trashFile(GBFile, boolean)}
     * @param file file
     * @throws ClientException
     */
    public void trashFile (GBFile file) throws ClientException {
        trashFile(file, file.isTrashed());
    }


    /**
     * Remove a file from the storage, even if it's not in the trash
     * @param file File to remove
     * @throws ClientException Exception thrown if the id is not valid
     */
    public abstract void removeFile (GBFile file) throws ClientException;

    /**
     * Add the listener for the SyncEvent received from the storage
     * @param listener Listener that will called with the relative event
     */
    public abstract void addSyncEventListener (SyncEventListener listener);

    /**
     * Remove the specified sync event listener
     * @param listener Listener to remove
     */
    public abstract void removeSyncEventListener (SyncEventListener listener);

    /**
     * Talk to the storage and tell to it the last ID of the event that
     * this client has heard. The not heard event will come as normal SyncEvent.
     * @param lastHeardId The ID of the last event you received or from you want
     *                    the list
     */
    public abstract void requestEvents (long lastHeardId);

    /**
     * Return the list of the shared files
     * @return List of the shared files
     * @throws ClientException
     */
    public abstract List<GBFile> getSharedFiles () throws ClientException;

    /**
     * Share o stop sharing a file
     * @param file File to share
     * @param share True to share, false to stop sharing
     * @throws ClientException
     */
    public abstract void share (GBFile file, boolean share) throws ClientException;

    /**
     * Make a search in the storage
     * @param filter Filter of the query
     * @return List of matching files
     * @throws ClientException
     */
    public abstract List<GBFile> getFilesByFilter (GBFilter filter) throws ClientException;

    /**
     * Return a list of recent files
     * @param from Offset of the result list
     * @param size Limit of the result list
     * @return List with the recent files
     * @throws ClientException
     */
    public abstract List<GBFile> getRecentFiles (long from, long size) throws ClientException;

    /**
     * Return a list of the trashed files
     * @return List with the trashed files
     * @throws ClientException
     */
    public abstract List<GBFile> getTrashedFiles () throws ClientException;

    /**
     * Empty the trash
     * @throws ClientException
     */
    public abstract void emptyTrash () throws ClientException;

    /**
     * Change the name of the file
     * @param file File whit the old name
     * @param newName New name of the file
     * @throws ClientException
     */
    public abstract void rename (GBFile file, String newName) throws ClientException;
}