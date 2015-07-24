package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.support.annotation.Nullable;
import nl.mpcjanssen.simpletask.task.Task;

import java.io.IOException;
import java.util.List;

/**
 * Interface definition of the storage backend used.
 *
 * Uses events to communicate with the application. Currently supported are SYNC_START, SYNC_DONE and FILE_CHANGED.
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
    List<Task> loadTasksFromFile(String path, @Nullable BackupInterface backup, String eol)  throws IOException;
    void startLogin(Activity caller, int i);
    void logout();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly);
    void saveTasksToFile(String path, List<Task> tasks, @Nullable BackupInterface backup, String eol) throws IOException;
    void appendTaskToFile(String path, List<Task> tasks, String eol) throws IOException;

    int getType();
    void sync();
    String readFile(String file, FileReadListener fileRead) throws IOException;
    boolean supportsSync();

    boolean isLoading();

    boolean changesPending();

    interface FileSelectedListener {
        void fileSelected(String file);
    }
    interface FileChangeListener {
        void fileChanged(String newName);
    }

    interface FileReadListener {
        void fileRead(String contents);
    }
}
