package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;
import android.support.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * Interface definition of the storage backend used.
 *
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
    List<String> loadTasksFromFile(String path, @Nullable BackupInterface backup, String eol)  throws IOException;
    void startLogin(Activity caller, int i);
    void logout();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly);
    void saveTasksToFile(String path, List<String> lines, @Nullable BackupInterface backup, String eol) throws IOException;
    void appendTaskToFile(String path, List<String> lines, String eol) throws IOException;

    int getType();
    void sync();
    String readFile(String file, FileReadListener fileRead) throws IOException;
    boolean supportsSync();
    boolean changesPending();

    boolean isLoading();

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
