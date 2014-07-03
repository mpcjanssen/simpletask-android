package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.TaskCache;

public class FileStore implements FileStoreInterface {


    public FileStore(Context todoApplication, String str) {

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
    public void append(String path, List<String> lines) {

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
    public void deauthenticate() {

    }

    @Override
    public void browseForNewFile(Activity act, String path, FileSelectedListener listener) {

    }

    @Override
    public void update(String mTodoName, List<String> original, List<String> updated) {

    }

    @Override
    public void delete(String mTodoName, List<String> strings) {

    }

    @Override
    public int getType() {
        return 0;
    }

    @Override
    public void move(String sourcePath, String targetPath, ArrayList<String> strings) {

    }

    public static String getDefaultPath() {
        return null;
    }
}
