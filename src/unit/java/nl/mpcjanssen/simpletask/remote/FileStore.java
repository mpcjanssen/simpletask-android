package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.lang.Override;
import java.util.ArrayList;
import java.util.List;

import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.TaskCache;

public class FileStore implements FileStoreInterface {


    public FileStore(Context todoApplication, String str) {

    }

    @Override
    public boolean isSyncing() {
        return false;
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public ArrayList<String> get(String path) {
        return null;
    }

    @Override
    public void archive(String path, List<String> lines) {

    }

    @Override
    public void startLogin(Activity caller, int i) {

    }

    @Override
    public void deauthenticate() {

    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener) {

    }

    @Override
    public void modify(String mTodoName, List<String> original, List<String> updated, List<String> added, List<String> removed) {

    }


    @Override
    public int getType() {
        return 0;
    }


    @Override
    public void setEol(String eol) {

    }

    @Override
    public void invalidateCache() {

    }

    @Override
    public boolean initialSyncDone() {
        return true;
    }

    public static String getDefaultPath() {
        return null;
    }
}
