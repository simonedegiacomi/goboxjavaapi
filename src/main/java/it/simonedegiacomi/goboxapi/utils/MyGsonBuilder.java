package it.simonedegiacomi.goboxapi.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.LongSerializationPolicy;

import java.lang.reflect.Modifier;

/**
 * @author Degiacomi Simone
 * Created on 21/02/16.
 */
public class MyGsonBuilder {

    public static Gson create () {
        GsonBuilder builder = new GsonBuilder();

        // Ignore static fields
        builder.excludeFieldsWithModifiers(Modifier.STATIC);

        // Ignore transient field
        builder.excludeFieldsWithModifiers(Modifier.TRANSIENT);

        builder.setLongSerializationPolicy(LongSerializationPolicy.STRING);

        return builder.create();
    }
}