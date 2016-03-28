package it.simonedegiacomi.goboxapi.utils;

/**
 * Created by Degiacomi Simone onEvent 27/12/2015.
 */
public class EasyHttpsException extends Exception {
    private final int responseCode;

    public EasyHttpsException(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }

    @Override
    public String toString() {
        return "EasyHttpsException{" +
                "responseCode=" + responseCode +
                '}';
    }
}
