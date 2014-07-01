package nl.mpcjanssen.simpletask.task;

import java.util.ArrayList;
import java.util.List;

import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.util.Util;

/**
 * Utility class which will execute the apply method on every task passed in via the
 * constructor, it will also store the updated tasks in the passed in FileStoreInterface (if it is non-null).
 */

abstract class TaskExecutor {
    private final ArrayList<String> mOriginalTasks;
    private List<Task> mTasks = null;
    private String mFilename = null;
    private FileStoreInterface mStore;

    public TaskExecutor (List<Task> tasks, FileStoreInterface fileStoreInterface, String filename) {
        this.mTasks = tasks;
        this.mOriginalTasks = Util.tasksToString(tasks);
        this.mStore = fileStoreInterface;
        this.mFilename = filename;
    }

    public List<Task> execute() {
        ArrayList<Task> results = new ArrayList<Task>();
        for (Task t : mTasks) {
            results.add(apply(t));
        }
        if (mStore!=null) {
            mStore.update(
                    mFilename,
                    mOriginalTasks,
                    Util.tasksToString(results));
        }
        return results;
    }
    abstract public Task apply (Task t);
}
