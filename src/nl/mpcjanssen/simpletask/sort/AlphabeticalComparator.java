package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;

import java.util.Comparator;

public class AlphabeticalComparator implements Comparator<Task> {
    @Override
    public int compare(Task a, Task b) {
        return a.getOriginalText().compareToIgnoreCase(b.getOriginalText());
    }
}
