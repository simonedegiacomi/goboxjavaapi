package it.simonedegiacomi.goboxapi;

import it.simonedegiacomi.goboxapi.authentication.Auth;
import it.simonedegiacomi.goboxapi.utils.URLBuilder;

import java.io.IOException;

/**
 * Created by simone on 28/03/16.
 */
public class Main {
    public static void main (String[] args) throws IOException {
        Auth auth = new Auth();
        auth.setMode(Auth.Modality.CLIENT);

        URLBuilder urls = new URLBuilder();
        urls.load();
    }
}
