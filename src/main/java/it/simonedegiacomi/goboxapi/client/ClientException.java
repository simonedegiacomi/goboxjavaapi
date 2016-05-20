package it.simonedegiacomi.goboxapi.client;

/**
 * This class is used to create the exceptions that a gobox client
 * can thrown.
 *
 * Created on 10/01/16.
 * @author Degiacomi Simone
 */
public class ClientException extends Exception {

    /**
     * Message that contains more information about the exception
     */
    private final String message;

    public ClientException (String message) {
        super(message);
        this.message = message;
    }

    public String getMessage () {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}