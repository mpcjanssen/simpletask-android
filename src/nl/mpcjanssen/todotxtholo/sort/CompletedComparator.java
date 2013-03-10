package nl.mpcjanssen.todotxtholo.sort;

import nl.mpcjanssen.todotxtholo.task.Task;

import java.util.Comparator;

public class CompletedComparator implements Comparator<Task> {


    @Override
    public int compare(Task a, Task b) {
        int completeA = a.isCompleted() ? 1 : 0;
        int completeB = b.isCompleted() ? 1 : 0;
        return (completeA - completeB);
    }
}
