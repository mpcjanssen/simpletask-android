package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import nl.mpcjanssen.simpletask.task.Task;

public class ContextComparator extends Ordering<Task> {

    private final AlphabeticalStringComparator mStringComparator;

    public ContextComparator (boolean caseSensitive) {
        super();
        this.mStringComparator = new AlphabeticalStringComparator(caseSensitive);
    }

    @Override
    public int compare(@NotNull Task a, @NotNull Task b) {
        List<String> contextsA = a.getLists();
        List<String> contextsB = b.getLists();

        if (contextsA.isEmpty() && contextsB.isEmpty()) {
            return 0;
        } else if (contextsA.isEmpty() && !contextsB.isEmpty()) {
            return -1;
        } else if (!contextsA.isEmpty() && contextsB.isEmpty()) {
            return 1;
        } else {
            Collections.sort(contextsA);
            Collections.sort(contextsB);
            return mStringComparator.compare(contextsA.get(0),contextsB.get(0));
        }
    }
}
