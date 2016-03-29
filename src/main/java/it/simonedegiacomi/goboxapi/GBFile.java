package it.simonedegiacomi.goboxapi;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;

/**
 * Class used to create the logic database representation
 * of a file.
 *
 * Created by Degiacomi Simone onEvent 24/12/2015.
 */
@DatabaseTable(tableName = "file")
public class GBFile {

    /**
     * The ID of the ROOT directory (well.. file)
     */
    public static final long ROOT_ID = 1;
    public static final long UNKNOWN_ID = -1;
    public static final long UNKNOWN_FATHER = UNKNOWN_ID;
    public static final GBFile ROOT_FILE = new GBFile(ROOT_ID, UNKNOWN_FATHER, "root", true);

    /**
     * Id of the file. Is not final because when the
     * file is created the ID is not known, but we now it
     * only when is inserted onEvent the database
     */
    @DatabaseField(generatedId = true, canBeNull = false)
    private long ID = UNKNOWN_ID;

    /**
     * Id of the father, 0 in case that the file is in
     * the root
     */
    @DatabaseField(canBeNull = false, columnName = "father_ID")
    private long fatherID = UNKNOWN_FATHER;

    /**
     * Indicate if the file is a 'real' file or
     * a directory
     */
    @DatabaseField(canBeNull = false, columnName = "is_directory")
    private boolean isDirectory;

    /**
     * Size of the file in bytes
     */
    @DatabaseField
    private long size;

    /**
     * Prefix of the absolute path of the file
     */
    private String prefix;

    /**
     * Name of the file
     */
    @DatabaseField(canBeNull = false)
    private String name;

    /**
     * Date of the creation of this file
     */
    @DatabaseField(columnName = "creation")
    private long creationDate;

    /**
     * Date of the last update of this file
     */
    @DatabaseField(columnName = "last_update")
    private long lastUpdateDate;

    /**
     * Path of the file
     * This path doesn't contains this file as last file, because Gson doesn't like
     * this... so i need to add the file every time the getPath method is called
     */
    private List<GBFile> path;

    @DatabaseField(columnName = "mime", dataType = DataType.STRING)
    private String mime;

    /**
     * List of children of this file
     */
    private List<GBFile> children;

    public GBFile () { }

    public GBFile (long id) {
        if(id < 0)
            throw new InvalidParameterException("The file id cannot be a negative value");

        this.ID = id;
    }

    /**
     * Create a new GBFile starting only with the name and the type of file (file or
     * folder). All the other fields are null
     * @param name Name of the file
     * @param isDirectory Type of file (folder or file)
     */
    public GBFile (String name, boolean isDirectory) {
        this.name = name;
        this.isDirectory = isDirectory;
    }

    /**
     * Create a new GBFile from a java file and a path prefix. This path prefix will
     * be removed from the path obtained from the java file and will not be included in the path,
     * this because the prefix doesn't make sense in the GoBox Storage. But, because you may need it,
     * when you call the method 'toFile' you'll get the same file you pass now tot he constructor (so
     * with the path prefix)
     * @param file Java file representation of the file
     * @param prefix Prefix to remove from the path
     */
    public GBFile (File file, String prefix) {
        this.name = file.getName();
        this.isDirectory = file.isDirectory();
        this.lastUpdateDate = file.lastModified();
        setAbsolutePathByString(file.toString(), prefix);
    }

    /**
     * Create a new file starting from the java representation. This method work just like
     * the (file, prefix) and doesn't remove anything from the path, so be careful!
     * @param file Java representation of the file
     */
    public GBFile (File file) {
        this(file, null);
    }

    /**
     * Create a new file
     * @param name Name of the new file
     * @param fatherID ID of hte father of the new file
     * @param isDirectory True if the file is a directory,
     *                    false otherwise
     */
    public GBFile(String name, long fatherID, boolean isDirectory) {
        this.name = name;
        this.fatherID = fatherID;
        this.isDirectory = isDirectory;
    }

    /**
     * Create a new file
     * @param name Name of the new file
     * @param fatherID ID of hte father of the new file
     * @param ID ID of the file
     * @param isDirectory True if the file is a directory,
     *                    false otherwise
     */
    public GBFile(long ID, long fatherID, String name, boolean isDirectory) {
        this.ID = ID;
        this.fatherID = fatherID;
        this.name = name;
        this.isDirectory = isDirectory;
    }

    /**
     * Return the ID of the file
     * @return The ID of the file
     */
    public long getID() {
        return ID;
    }

    /**
     * Set the ID of the file.
     * Only the classes inside this package, or that
     * extends this class should edit the ID of the file
     * @param ID ID of the file
     */
    public void setID(long ID) {
        this.ID = ID;
    }

    /**
     * Set the ID of the father.
     * Only the classes inside this package, or that
     * extends this class should edit the ID of the file
     * @param fatherID ID of the father
     */
    public void setFatherID(long newFatherID) {
        this.fatherID = newFatherID;
    }

    /**
     * Return the ID of the file
     * @return ID of the father
     */
    public long getFatherID() {
        return fatherID;
    }

    /**
     * Return the size of the file expressed in bytes
     * @return Size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Set the size of the file. If is called, is a good idea
     * to change also the lastUpdateDate
     * @param size The size of the file
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * Return the name of the file
     * @return Name of the file
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Return true if the file is a directory, false otherwise
     * @return Identity of the file
     */
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     * Return the date if the last update of this file
     * @return Date expressed in milliseconds
     */
    public long getLastUpdateDate() {
        return lastUpdateDate;
    }

    /**
     * Set the last update of this file.
     * @param lastUpdateDate date of the last update in
     *                       milliseconds
     */
    public void setLastUpdateDate(long lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    /**
     * Return the date of the creation of this file
     * @return Date of the creation of this file in milliseconds
     */
    public long getCreationDate() {
        return creationDate;
    }

    /**
     * Set the creation date
     * @param creationDate Date of the creation in milliseconds
     */
    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Generate a new GBFile, with his fatherID equals to this id
     * @param name Name of the new file
     * @param isDirectory is a directory or a  file?
     * @return The new generated file, son of this file
     */
    public GBFile generateChild (String name, boolean isDirectory) {
        return new GBFile(name, ID, isDirectory);
    }

    /**
     * Return the Path as a list of 'piece'. The last piece is this file
     * @return List representation of the path including this file
     */
    public List<GBFile> getPathAsList() {
        // Create the list
        LinkedList<GBFile> temp = new LinkedList<>();

        // If the path is defined, add all the pieces
        if(path != null)
            temp.addAll(path);

        // Add the name of this file
        temp.add(this);

        // Return the path as list
        return temp;
    }

    /**
     * Return the Path as a list of 'piece'. The last piece is this file
     * @return List representation of the path including this file
     */
    public List<GBFile> getAbsolutePathAsList() {
        // Get the relative path
        List<GBFile> temp = getPathAsList();

        String[] pieces = prefix.split("/");

        for (int i = pieces.length - 1;i >= 0;i--)
                temp.add(0, new GBFile(pieces[i], true));

        // Return the path as list
        return temp;
    }

    private static String stringify (List<GBFile> list) {
        StringBuilder builder = new StringBuilder();

        // Add every piece of the path
        boolean first = true;
        for(GBFile piece : list) {
            // If it's not the first piece...
            if(!first)
                builder.append('/'); // add the slash

            // Add the piece
            builder.append(piece.getName());

            if (first)
                first = false;
        }

        // Return the path as string
        return builder.toString();
    }

    /**
     * Return the path of the file as a string. This path contains the name of this file as last
     * node and the specified prefix passed as argument
     * @param prefix Prefix to add to the generated path
     * @return String path of the file
     */
    public String getPathAsString () {
        return stringify(getPathAsList());
    }

    public String getAbsolutePathAsString () {
        return stringify(getAbsolutePathAsList());
    }

    /**
     * Set the path of the file, without updating the fatherID
     * @param pieces New path of the file. This list need to contains this file
     *               as last node
     */
    public void setPathByList (List<GBFile> pieces) {
        this.path = pieces;

        // Remove this file from the path
        GBFile myself = pieces.remove(pieces.size() - 1);

        this.name = myself.name;
    }

    public void setAbsolutePathByList (List<GBFile> pieces, String prefix) {
        this.prefix = prefix;

        // Create a new list that holds the nodes
        path = new LinkedList<>();

        String[] badPieces = prefix.split("/");

        // skip the initials bad nodes
        int i = 0;
        while(i < badPieces.length && i < pieces.size() && pieces.get(i).equals(badPieces[i]))
            i++;

        if(i == pieces.size()) {
            // All the path is the file! this means that this is the root!
            this.ID = ROOT_FILE.ROOT_ID;
            this.fatherID = ROOT_FILE.UNKNOWN_ID;
            this.isDirectory = ROOT_FILE.isDirectory;
            return;
        }

        // Add all the other except the last
        while(i < pieces.size() - 1)
            path.add(pieces.get(i++));

        GBFile myself = pieces.get(pieces.size());
        this.name = myself.name;
    }

    /**
     * Set the relative path to the gobox files folder
     * @param pathString Path fo the file relative to the gobox files folder
     */
    public void setPathByString (String pathString) {
        path = new LinkedList<>();
        String[] stringPieces = pathString.split("/");

        for(int i = stringPieces.length - 1; i > 0 ;i--)
            path.add(new GBFile(stringPieces[i], true));

        this.name = stringPieces[stringPieces.length - 1];
    }

    /**
     * Set the absolute path of the file.
     * @param pathString Absolute path of the file
     * @param prefix Prefix to remove from the absolute path of the file to get the relative path of the
     *               gobox files folder
     */
    public void setAbsolutePathByString (String pathString, String prefix) {
        this.prefix = prefix;
        setPathByString(pathString.substring(prefix.length(), pathString.length()));
    }

    /**
     * Return the java io.File reference to this file. This file is created using the absolute path
     * @return Reference to this file
     */
    public File toFile () {
        return new File(getAbsolutePathAsString());
    }

    /**
     * Return the children of this file
     * @return
     */
    public List<GBFile> getChildren() {
        return children;
    }

    /**
     * Set the children of this file, removing (logically) the previous
     * @param children New list of children
     */
    public void setChildren(List<GBFile> children) {
        this.children = children;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    /**
     * Return the prefix currently used to create the absolute path of the file
     * @return Current prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Set the prefix to create the absolute path of the file
     * @param prefix Prefix to use to create the absolute file path
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Return the string representation of this file
     * @return Representation of this file
     */
    @Override
    public String toString() {
        return "GBFile{" +
                "ID=" + ID +
                ", fatherID=" + fatherID +
                ", isDirectory=" + isDirectory +
                ", size=" + size +
                ", name='" + name + '\'' +
                ", creationDate=" + creationDate +
                ", lastUpdateDate=" + lastUpdateDate +
                ", mime='" + mime + '\'' +
                '}';
    }
}