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
    ArrayList<String> get(TaskBag.Preferences preferences);
    void store(String data);
    void append(String path, String data);
    void startLogin(Activity caller, int i);
    void startWatching(LocalBroadcastManager broadCastManager, Intent intent);
    void stopWatching();
    boolean supportsAuthentication();
    void deauthenticate();
    boolean isLocal();
    void browseForNewFile(Activity act, FileSelectedListener listener);
    void init(Context ctx, String todoFile);

    public interface FileSelectedListener {
        void fileSelected(String file);
    }
    public interface DirectorySelectedListener {
        void directorySelected(File directory);
    }
}
