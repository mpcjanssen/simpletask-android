package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import java.io.File;
import java.util.ArrayList;

import nl.mpcjanssen.simpletask.LoginScreen;
import nl.mpcjanssen.simpletask.task.TaskBag;

/**
 * Created by a156712 on 10-6-2014.
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
    ArrayList<String> get(String path, TaskBag.Preferences preferences);
    void store(String path, String data);
    boolean append(String path, String data);
    void startLogin(Activity caller, int i);
    void startWatching(String path);
    void stopWatching(String path);
    boolean supportsAuthentication();
    void deauthenticate();
    boolean isLocal();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener);

    public interface FileSelectedListener {
        void fileSelected(String file);
    }
}
