package nl.mpcjanssen.todotxtholo.sort;

import nl.mpcjanssen.todotxtholo.task.Task;

import java.util.Comparator;

public class AlphabeticalComparator implements Comparator<Task> {
    @Override
    public int compare(Task a, Task b) {
        return a.getOriginalText().compareToIgnoreCase(b.getOriginalText());
    }
}
