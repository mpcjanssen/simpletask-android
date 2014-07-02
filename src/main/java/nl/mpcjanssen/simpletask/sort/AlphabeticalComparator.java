package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.Token;

public class AlphabeticalComparator extends ReversableComparator {
    public AlphabeticalComparator(boolean reverse) {
        super(reverse);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        return a.inScreenFormat(Token.TEXT).compareToIgnoreCase(b.inScreenFormat(Token.TEXT));
    }
}
