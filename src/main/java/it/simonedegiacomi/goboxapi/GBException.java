package it.simonedegiacomi.goboxapi;

/**
 * Created on 10/01/16.
 * @author Degiacomi Simone
 */
public class GBException extends Exception {

    /**
     * Message that contains more information about the exception
     */
    private final String message;

    public GBException (String message) {
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