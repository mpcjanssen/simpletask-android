package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.NotNull;

import nl.mpcjanssen.simpletask.task.Task;

public class CompletedComparator extends Ordering<Task> {

    @Override
    public int compare(@NotNull Task a, @NotNull Task b) {
        int completeA = a.isCompleted() ? 1 : 0;
        int completeB = b.isCompleted() ? 1 : 0;
        return (completeA - completeB);
    }
}
