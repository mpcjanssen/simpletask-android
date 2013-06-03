package nl.mpcjanssen.todotxtholo.sort;

import nl.mpcjanssen.todotxtholo.task.Task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContextComparator extends ReversableComparator {

    @Override
    public int unreversedCompare(Task a, Task b) {
        List<String> contextsA = a.getContexts();
        List<String> contextsB = b.getContexts();

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
