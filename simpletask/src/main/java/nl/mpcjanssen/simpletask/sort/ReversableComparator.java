package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;

import java.util.Comparator;

abstract class ReversableComparator implements Comparator<Task> {
    private final boolean reversed;

    public ReversableComparator(boolean reversed) {
        this.reversed = reversed;
    }

    public ReversableComparator() {
        this.reversed = false;
    }


    abstract int unreversedCompare (Task a, Task b);

    @Override
    public int compare(Task a, Task b) {
        if (reversed){
            return unreversedCompare(b,a);
        } else {
            return unreversedCompare(a,b);
        }
    }
}
