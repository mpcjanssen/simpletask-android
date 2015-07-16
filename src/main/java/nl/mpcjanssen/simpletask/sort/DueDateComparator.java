package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;
import nl.mpcjanssen.simpletask.task.Task;

public class DueDateComparator extends Ordering<Task> {

    @Override
    public int compare(Task a, Task b) {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }
        int result;
        if (a.getDueDate() == null && b.getDueDate() == null) {
            result = 0;
        } else if (a.getDueDate() == null) {
            result = 1;
        } else if (b.getDueDate() == null) {
            result = -1;
        } else {
            result = a.getDueDate().compareTo(b.getDueDate());
        }
        return result;
    }
}
