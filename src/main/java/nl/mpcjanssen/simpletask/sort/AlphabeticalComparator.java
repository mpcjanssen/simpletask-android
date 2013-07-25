package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;

public class AlphabeticalComparator extends ReversableComparator {
    public AlphabeticalComparator(boolean reverse) {
        super(reverse);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        return a.getText().compareToIgnoreCase(b.getText());
    }
}
