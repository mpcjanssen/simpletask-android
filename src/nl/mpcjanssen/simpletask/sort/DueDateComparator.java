package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;

public class DueDateComparator extends ReversableComparator {

    public DueDateComparator(boolean b) {
        super(b);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        int result = 0;
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
