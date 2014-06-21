package nl.mpcjanssen.simpletask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;

import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.TaskBag;

public class FileStore implements FileStoreInterface {


    public FileStore(Context todoApplication, LocalBroadcastManager localBroadcastManager, Intent intent) {

    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public ArrayList<String> get(String path, TaskBag.Preferences preferences) {
        return null;
    }

    @Override
    public void store(String path, String data) {

    }

    @Override
    public boolean append(String path, String data) {
        return false;
    }

    @Override
    public void startLogin(Activity caller, int i) {

    }

    @Override
    public void startWatching(String path) {

    }

    @Override
    public void stopWatching(String path) {

    }

    @Override
    public boolean supportsAuthentication() {
        return false;
    }

    @Override
    public void deauthenticate() {

    }

    static public String getDefaultPath() {
        return "";
    }

    @Override
    public boolean isLocal() {
        return false;
    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener) {

    }
}
