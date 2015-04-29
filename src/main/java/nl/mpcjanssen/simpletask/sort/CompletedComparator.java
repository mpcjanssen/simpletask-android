package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import nl.mpcjanssen.simpletask.task.Task;

public class CompletedComparator extends Ordering<Task> {

    @Override
    public int compare(Task a, Task b) {
        if (a==null) return 1;
        if (b==null) return -1;

        int completeA = a.isCompleted() ? 1 : 0;
        int completeB = b.isCompleted() ? 1 : 0;
        return (completeA - completeB);
    }
}
