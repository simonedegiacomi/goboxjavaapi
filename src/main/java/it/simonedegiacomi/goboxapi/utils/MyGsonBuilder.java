package it.simonedegiacomi.goboxapi.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author Degiacomi Simone
 * Created on 21/02/16.
 */
public class MyGsonBuilder {

    /**
     * Generate a new gson object configured with the gobox settings
     * @return New gson object
     */
    public static Gson create () {

        // Create a new Gson Builder
        GsonBuilder builder = new GsonBuilder();

        builder.excludeFieldsWithoutExposeAnnotation();

        return builder.create();
    }
}