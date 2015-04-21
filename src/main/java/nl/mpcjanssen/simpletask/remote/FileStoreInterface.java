package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interface definition of the storage backend used.
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
    void loadTasksFromFile (String path, TaskCache taskCache)  throws IOException;
    void startLogin(Activity caller, int i);
    void deauthenticate();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly);
    void saveTasksToFile(String path, TaskCache taskCache);
    void appendTaskToFile(String path, List<Task> tasks) throws IOException;

    int getType();
    void setEol(String eol);
    boolean initialSyncDone();
    void sync();
    String readFile(String file);
    boolean supportsSync();
    interface FileSelectedListener {
        void fileSelected(String file);
    }
}
