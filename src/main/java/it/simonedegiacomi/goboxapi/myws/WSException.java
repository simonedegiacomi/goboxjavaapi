package it.simonedegiacomi.goboxapi.myws;

public class WSException extends Exception {
    private final String message;

    @Override
    public String getMessage() {
        return message;
    }

    public WSException(String message) {
        super(message);
        this.message = message;
    }
}