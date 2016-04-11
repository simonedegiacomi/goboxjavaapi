package it.simonedegiacomi.goboxapi.myws;

import com.google.gson.JsonElement;

/**
 * Listener for the response ogf a query
 *
 * Created on 31/12/2015.
 * @author Degiacomi Simone
 */
public interface WSQueryResponseListener {

    /**
     * Method that will be called when the response of a
     * query is retrieved
     * @param response Response of the query
     */
    public void onResponse (JsonElement response);
}
