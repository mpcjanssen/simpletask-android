package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;

import java.io.IOException;
import java.util.List;

/**
 * Interface definition of the storage backend used.
 *
 * Uses events to communicate with the application. Currently supported are SYNC_START, SYNC_DONE and FILE_CHANGED.
 */
public interface FileStoreInterface {
    boolean isAuthenticated();
    TodoList loadTasksFromFile(String path, TodoList.TodoListChanged todoListChanged)  throws IOException;
    void startLogin(Activity caller, int i);
    void deauthenticate();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly);
    void saveTasksToFile(String path, TodoList todoList);
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
