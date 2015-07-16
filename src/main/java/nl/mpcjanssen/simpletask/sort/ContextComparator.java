package nl.mpcjanssen.simpletask.sort;

import android.support.annotation.NonNull;
import com.google.common.collect.Ordering;
import nl.mpcjanssen.simpletask.task.Task;

import java.util.Collections;
import java.util.List;

public class ContextComparator extends Ordering<Task> {

    private final AlphabeticalStringComparator mStringComparator;

    public ContextComparator (boolean caseSensitive) {
        super();
        this.mStringComparator = new AlphabeticalStringComparator(caseSensitive);
    }

    @Override
    public int compare(Task a, Task b) {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }
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
