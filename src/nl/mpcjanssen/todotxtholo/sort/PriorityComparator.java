package nl.mpcjanssen.todotxtholo.sort;

import nl.mpcjanssen.todotxtholo.task.Priority;
import nl.mpcjanssen.todotxtholo.task.Task;

import java.util.Comparator;

public class PriorityComparator implements Comparator<Task> {
    @Override
    public int compare(Task a, Task b) {
        Priority prioA = a.getPriority();
        Priority prioB = b.getPriority();

        if (prioA.getCode().equals(prioB.getCode())) {
            return 0;
        } else if (prioA.inFileFormat().equals("")) {
            return 1;
        } else if (prioB.inFileFormat().equals("")) {
            return -1;
        } else {
            return prioA.compareTo(prioB);
        }
    }
}
