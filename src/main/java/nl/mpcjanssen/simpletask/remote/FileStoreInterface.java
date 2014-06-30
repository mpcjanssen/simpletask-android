package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;

import java.util.ArrayList;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;

/**
 * Interface definition of the storage backend used.
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
    ArrayList<String> get(String path, TaskCache.Preferences preferences);
    void store(String path, ArrayList<String> lines);
    void append(String path, ArrayList<String> lines);
    void append(String path, String tasks);
    void startLogin(Activity caller, int i);
    void startWatching(String path);
    void stopWatching(String path);
    boolean supportsAuthentication();
    void deauthenticate();
    boolean isLocal();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener);
    void update(String o, String s);
    void delete(String mTodoName, ArrayList<String> strings);
    public interface FileSelectedListener {
        void fileSelected(String file);
    }
}
