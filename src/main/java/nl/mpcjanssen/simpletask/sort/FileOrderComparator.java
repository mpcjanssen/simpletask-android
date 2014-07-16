package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.NotNull;

import nl.mpcjanssen.simpletask.task.Task;

public class FileOrderComparator extends Ordering<Task> {

    @Override
    public int compare(@NotNull Task a, @NotNull Task b) {
        if (a.getId() < b.getId()) {
            return -1;
        } else {
            return 1;
        }
    }
}
