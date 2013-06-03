package nl.mpcjanssen.todotxtholo.sort;

import nl.mpcjanssen.todotxtholo.task.Task;

import java.util.Comparator;

public class AlphabeticalComparator extends ReversableComparator {
    @Override
    public int unreversedCompare(Task a, Task b) {
        return a.getOriginalText().compareToIgnoreCase(b.getOriginalText());
    }
}
