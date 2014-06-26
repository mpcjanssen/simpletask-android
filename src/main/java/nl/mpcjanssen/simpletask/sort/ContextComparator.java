package nl.mpcjanssen.simpletask.sort;

import java.util.Collections;
import java.util.List;

import nl.mpcjanssen.simpletask.task.Task;

public class ContextComparator extends ReversableComparator {

    public ContextComparator(boolean reverse) {
       super(reverse);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
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
            return contextsA.get(0).compareToIgnoreCase(contextsB.get(0));
        }
    }
}
