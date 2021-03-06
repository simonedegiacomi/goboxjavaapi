package it.simonedegiacomi.goboxapi.myws;

import com.google.gson.JsonElement;

import java.util.concurrent.Callable;

/**
 * Implementation of Callable used in my implementation
 * of handlers socket client for the java FutureTask
 *
 * Created on 10/01/16.
 * @author Degiacomi Simone
 */
public class WSCallable implements Callable<JsonElement> {

    /**
     * This field contains the response retrieve from the server
     */
    private JsonElement response;

    /**
     * Set the response. This method must be called before execute the call method
     * @param response Response
     */
    public void setResponse (JsonElement response) {
        this.response = response;
    }

    @Override
    public JsonElement call() throws Exception {

        // This is a very hard task...
        return response;
    }
}
