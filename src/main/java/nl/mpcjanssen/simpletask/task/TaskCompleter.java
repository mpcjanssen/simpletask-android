package nl.mpcjanssen.simpletask.task;

import java.util.List;
import java.util.TimeZone;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;

public class TaskCompleter extends TaskExecutor {

    private boolean mComplete;

    public TaskCompleter(boolean complete, List<Task> tasks, FileStoreInterface fileStoreInterface, String filename) {
        super(tasks, fileStoreInterface, filename);
        this.mComplete = complete;
    }

    @Override
    public Task apply(Task t) {
        if (mComplete) {
            DateTime now = DateTime.today(TimeZone.getDefault());
            t.markComplete(now);
        } else {
            t.markIncomplete();
        }
        return t;
    }
}
