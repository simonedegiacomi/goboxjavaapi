package it.simonedegiacomi.goboxapi;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.List;

/**
 * Class used to create the logic database representation of a file in the gobox environment.
 * This class can also be used with Gson to serialize an instance in json or viceversa
 *
 * @author Degiacomi Simone
 * Created on 24/12/2015.
 */
@DatabaseTable(tableName = "file")
public class GBFile {

    /**
     * The ID of the root is 1
     */
    public static final long ROOT_ID = 1;

    /**
     * The fake root father ID is 0
     */
    public static final long ROOT_FATHER_ID = 0;

    /**
     * Any file that don't know his ID will get a negative ID (-1)
     */
    public static final long UNKNOWN_ID = -1;

    /**
     * Sample root file, that has 1 {@link #ROOT_ID} as ID , 0 {@link #ROOT_FATHER_ID} as father ID, is a folder and
     * hasn't a name
     */
    public static final GBFile ROOT_FILE = new GBFile(ROOT_ID, ROOT_FATHER_ID, "", true);

    /**
     * Id of the file. Is not final because when the file is created the ID is not known, but we now it only when is
     * inserted on the database. If the file doesn't know his ID, this field must be {@link #UNKNOWN_ID}.
     */
    @DatabaseField(generatedId = true, canBeNull = false)
    private long ID = UNKNOWN_ID;

    /**
     * Id of the father, 0 {@link #ROOT_FATHER_ID} in case that the file is in the root
     */
    @DatabaseField(canBeNull = false, columnName = "father_ID")
    private long fatherID = UNKNOWN_ID;

    /**
     * Indicate if the file is a file or a directory
     */
    @DatabaseField(canBeNull = false, columnName = "is_directory")
    private boolean isDirectory;

    /**
     * Indicate if the file is trashed or not
     */
    @DatabaseField(canBeNull = false)
    private boolean trashed = false;

    /**
     * Size of the file in bytes
     */
    @DatabaseField
    private long size;

    /**
     * Prefix of the absolute path of the file
     */
    private transient String prefix;

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
     * Path of the file.
     * NOTE taht this path doesn't contains this file as last file, because Gson doesn't like this... so i need to add
     * the file every time the {@link #getPathAsList()} method is called
     */
    private List<GBFile> path;

    /**
     * Type of file
     */
    @DatabaseField(columnName = "mime", dataType = DataType.STRING)
    private String mime;

    /**
     * List of children of this file
     */
    private List<GBFile> children;

    /**
     * Empty constructor (used by Gson)
     */
    public GBFile () { }

    public GBFile (long id) {
        this(id, UNKNOWN_ID, null, false);
    }

    /**
     * Create a new GBFile starting only with the name and the type of file (file or folder).
     * Using this constructor will set a null path, but if the name contains any '/', the method {@link #setPathByString(String)}
     * will be called
     * @param name Name or path of the file
     * @param isDirectory Type of file (folder or file)
     */
    public GBFile (String name, boolean isDirectory) {
        this(UNKNOWN_ID, UNKNOWN_ID, name, isDirectory);
    }

    /**
     * Create a new GBFile from a java.io.File and a path prefix. This path prefix will be removed from the path obtained
     * from the java.io.File and will not be included in the path, this because the prefix doesn't make sense in the GoBox
     * Storage.
     * NOTE that When you'll call the {@link #toFile()} method, you'll get a new instance of java.io.File equals to this.
     * @param file Java file representation of the file
     * @param prefix Prefix to remove from the path
     */
    public GBFile (File file, String prefix) {
        setAbsolutePathByString(file.toString(), prefix);

        // If the file exist, check if it's a folder
        if (file.exists()) {
            this.isDirectory = file.isDirectory();
            this.lastUpdateDate = file.lastModified();
        }
    }

    /**
     * Create a new file starting from the java representation. This method work just like the {@link #GBFile(File, String)}
     * but doesn't remove anything from the path, so be careful
     * @param file Java representation of the file
     */
    public GBFile (File file) {
        this(file, null);
    }

    /**
     * Create a new file. This method call {@link #GBFile(String, boolean)} constructor
     * @param name Name of the new file
     * @param fatherID ID of hte father of the new file
     * @param isDirectory True if the file is a directory, false otherwise
     */
    public GBFile(String name, long fatherID, boolean isDirectory) {
        this(UNKNOWN_ID, fatherID, name, isDirectory);
    }

    /**
     * Create a new file
     * @param name Name of the new file
     * @param fatherID ID of hte father of the new file
     * @param ID ID of the file
     * @param isDirectory True if the file is a directory, false otherwise
     */
    public GBFile(long ID, long fatherID, String name, boolean isDirectory) {
        if((ID < 0 && ID != UNKNOWN_ID) || (fatherID < 0 && fatherID != UNKNOWN_ID))
            throw new InvalidParameterException("Id cannot be a negative value");

        this.ID = ID;
        this.fatherID = fatherID;
        this.isDirectory = isDirectory;
        if (name != null)
            setName(name);
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
     * @param ID ID of the file
     */
    public void setID(long ID) {
        this.ID = ID;
    }

    /**
     * Set the ID of the father.
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
     * Return true if the file is trashed
     * @return true if the file is trashed
     */
    public boolean isTrashed() {
        return trashed;
    }

    /**
     * Trash the file
     * @param trashed Trash the fiel if true
     */
    public void setTrashed(boolean trashed) {
        this.trashed = trashed;
    }

    /**
     * Set the size of the file. If is called, is a good idea to call also the {@link #setLastUpdateDate(long)} method
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

    /**
     * Set the name of the file. If the name specified as parameter contains any '/' the method {@link #setPathByString(String)}
     * will be called
     * @param name Name of the file
     */
    public void setName(String name) {
        if (name.contains("/"))
            setPathByString(name);
        else
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

        // Assert that this file is a folder
        if (!this.isDirectory)
            throw new IllegalStateException("This file is not a folder");

        // Create the new child
        GBFile child = new GBFile(name, ID, isDirectory);
        child.setPathByString(new StringBuilder(getPathAsString()).append('/').append(name).toString());
        return child;
    }

    /**
     * Return the Path as a list of 'piece'. If the name of this file is not null and his length is more than 0
     * the last piece of the list is a reference to this file. This list doesn't contain the absolute prefix.
     * @return List representation of the path including this file
     */
    public List<GBFile> getPathAsList() {

        if (path == null)
            return null;

        // Create the list
        LinkedList<GBFile> temp = new LinkedList<>();

        temp.addAll(path);

        // Add the name of this file
        if(name != null && name.length() > 0)
            temp.add(this);

        // Return the path as list
        return temp;
    }

    /**
     * Return the Path as a list of 'piece'. The last piece is this file. This list also contains the absolute prefix.
     * @return List representation of the path including this file
     */
    public List<GBFile> getAbsolutePathAsList() {
        // Get the relative path
        List<GBFile> temp = getPathAsList();

        if (temp == null)
            return null;

        if (prefix != null) {
            String[] pieces = prefix.split("/");

            for (int i = pieces.length - 1; i >= 0; i--)
                temp.add(0, new GBFile(pieces[i], true));
        }

        // Return the path as list
        return temp;
    }

    /**
     * This method create a that represent the list of file.
     * @param list List of file that represent the path
     * @return String that represent the list
     */
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
     * Return the path of the file as a string. This path contains the name of this file as last node
     * @return String path of the file
     */
    public String getPathAsString () {
        return stringify(getPathAsList());
    }

    /**
     * Return the path of this file as a string. This string will contain also the absolute prefix and the name of this
     * file at the end
     * @return String that represents the absolute path of this file
     */
    public String getAbsolutePathAsString () {
        return stringify(getAbsolutePathAsList());
    }

    /**
     * Set the path and the name of this file.
     * NOTE that this list need to contains this file as last node
     * @param pieces New path of the file.
     */
    public void setPathByList (List<GBFile> pieces) {
        this.path = pieces;

        // Remove this file from the path
        GBFile myself = pieces.remove(pieces.size() - 1);

        this.name = myself.name;
    }

    /**
     * Set the path, the prefix and the name of this file. This list must contain the prefix, the path and a reference to
     * this file as last element.
     * @param pieces Path as list
     * @param prefix Absolute prefix
     */
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
            this.fatherID = ROOT_FILE.ROOT_FATHER_ID;
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
     * Set the relative (to the gobox files folder) path and the name of this file.
     * NOTE that this string must contain the name of this file at the end
     * @param pathString Path of this file
     */
    public void setPathByString (String pathString) {
        path = new LinkedList<>();

        if (pathString.length() <= 0 || (pathString.length() == 1 && pathString.charAt(0) == '/') ) {
            this.name = pathString;
            return;
        }

        String[] stringPieces = pathString.split("/");

        for(int i = 0; i < stringPieces.length - 1 ;i++)
            path.add(new GBFile(stringPieces[i], true));

        this.name = stringPieces[stringPieces.length - 1];
    }

    /**
     * Set the absolute prefix, relative (to the gobox files folder) path and name of this file.
     * @param pathString Absolute path of the file
     * @param prefix Prefix to remove from the absolute path of the file to get the relative path of the gobox files folder
     */
    public void setAbsolutePathByString (String rawPathString, String prefix) {
        this.prefix = prefix;
        String pathString = rawPathString;
        if(prefix != null) {
            if (prefix.length() >= rawPathString.length()) {
                pathString = new String();
                this.ID = ROOT_ID;
            } else {
                pathString = pathString.substring(prefix.length(), pathString.length());
            }
        }

        setPathByString(pathString);
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

    @Override
    public boolean equals (Object b) {
        if (b == null)
            return false;
        if (b == this)
            return true;
        if (! (b instanceof GBFile))
            return false;

        GBFile otherFile = (GBFile) b;
        if (otherFile.ID != UNKNOWN_ID && this.ID != UNKNOWN_ID && otherFile.ID == this.ID)
            return true;

        if(otherFile.name != null && this.name != null && !otherFile.name.equals(this.name))
            return false;

        if(otherFile.fatherID != UNKNOWN_ID && this.fatherID != UNKNOWN_ID && otherFile.fatherID != this.fatherID)
            return false;

        if(otherFile.name != null && this.name != null && !otherFile.name.equals(this.name))
            return false;

        if(otherFile.isDirectory != this.isDirectory)
            return false;

        return true;
    }
}