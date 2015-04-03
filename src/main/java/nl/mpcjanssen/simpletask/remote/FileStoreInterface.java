package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface definition of the storage backend used.
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
    @Nullable
    List<String> get(String path);
    void archive(String path, List<String> lines);
    void startLogin(Activity caller, int i);
    void deauthenticate();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly);
    void modify(String mTodoName, List<String> original,
                List<String> updated,
                List<String> added,
                List<String> removed);
    int getType();
    void setEol(String eol);
    boolean isSyncing();
    public boolean initialSyncDone();
    void invalidateCache();
    void sync();
    String readFile(String file);
    boolean supportsSync();
    public interface FileSelectedListener {
        void fileSelected(String file);
    }
}
