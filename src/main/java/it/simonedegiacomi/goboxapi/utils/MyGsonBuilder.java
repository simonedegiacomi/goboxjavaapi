package it.simonedegiacomi.goboxapi.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
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
//        builder.setExclusionStrategies(new ExclusionStrategy() {
//            @Override
//            public boolean shouldSkipField(FieldAttributes field) {
//                field.
//                return field.getAnnotation(GBFileIDReference.class) != null;
//            }
//
//            @Override
//            public boolean shouldSkipClass(Class<?> clazz) {
//                return false;
//            }
//        });


        return builder.create();
    }
}