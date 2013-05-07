package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContextComparator implements Comparator<Task> {


    @Override
    public int compare(Task a, Task b) {
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
