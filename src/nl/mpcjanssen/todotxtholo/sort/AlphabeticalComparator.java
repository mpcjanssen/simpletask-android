package nl.mpcjanssen.todotxtholo.sort;

import nl.mpcjanssen.todotxtholo.task.Task;

public class AlphabeticalComparator extends ReversableComparator {
    public AlphabeticalComparator(boolean reverse) {
        super(reverse);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        return a.getOriginalText().compareToIgnoreCase(b.getOriginalText());
    }
}
