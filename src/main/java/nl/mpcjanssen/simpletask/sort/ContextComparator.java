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
    public int compare(@NonNull Task a, @NonNull Task b) {
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
