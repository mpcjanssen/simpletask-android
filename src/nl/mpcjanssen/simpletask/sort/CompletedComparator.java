package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;

public class CompletedComparator extends ReversableComparator {

    public CompletedComparator(boolean reverse) {
        super(reverse);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        int completeA = a.isCompleted() ? 1 : 0;
        int completeB = b.isCompleted() ? 1 : 0;
        return (completeA - completeB);
    }
}
