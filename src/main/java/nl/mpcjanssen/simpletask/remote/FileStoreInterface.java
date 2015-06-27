package nl.mpcjanssen.simpletask.remote;

import android.app.Activity;

import android.support.annotation.Nullable;
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
    TodoList loadTasksFromFile(String path, TodoList.TodoListChanged todoListChanged, @Nullable BackupInterface backup)  throws IOException;
    void startLogin(Activity caller, int i);
    void logout();
    void browseForNewFile(Activity act, String path, FileSelectedListener listener, boolean txtOnly);
    void saveTasksToFile(String path, TodoList todoList, @Nullable BackupInterface backup) throws IOException;
    void appendTaskToFile(String path, List<Task> tasks) throws IOException;

    int getType();
    void setEol(String eol);
    void sync();
    String readFile(String file, FileReadListener fileRead) throws IOException;
    boolean supportsSync();

    boolean isLoading();

    interface FileSelectedListener {
        void fileSelected(String file);
    }
    public interface FileChangeListener {
        void fileChanged(String newName);
    }

    public interface FileReadListener {
        void fileRead(String contents);
    }
}
