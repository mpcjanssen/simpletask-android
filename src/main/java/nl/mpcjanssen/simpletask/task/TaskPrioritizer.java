package nl.mpcjanssen.simpletask.task;

import java.util.List;

import nl.mpcjanssen.simpletask.remote.FileStoreInterface;

/**
 *
 */
public class TaskPrioritizer extends TaskExecutor {
    private Priority mPrio;

    public TaskPrioritizer(Priority prio, List<Task> tasks, FileStoreInterface fileStoreInterface, String filename) {
        super(tasks, fileStoreInterface, filename);
        this.mPrio = prio;
    }

    @Override
    public Task apply(Task t) {
        t.setPriority(mPrio);
        return t;
    }
}
