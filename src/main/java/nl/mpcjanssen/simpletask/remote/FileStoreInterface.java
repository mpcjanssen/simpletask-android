package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;

import java.util.ArrayList;
import java.util.List;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;

/**
 * Interface definition of the storage backend used.
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
    ArrayList<String> get(String path);
    void append(String path, List<String> lines);
    void append(String path, String tasks);
    void startLogin(Activity caller, int i);
    void startWatching(String path);
    void stopWatching(String path);
    void deauthenticate();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener);
    void update(String mTodoName, String original, String updated);
    void update(String mTodoName, List<String> original, List<String> updated);
    void delete(String mTodoName, List<String> strings);
    int getType();
    public interface FileSelectedListener {
        void fileSelected(String file);
    }
}
