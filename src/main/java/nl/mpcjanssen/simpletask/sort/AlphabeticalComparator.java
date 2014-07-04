package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.token.Token;

public class AlphabeticalComparator extends ReversableComparator {
    public AlphabeticalComparator(boolean reverse) {
        super(reverse);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        return a.showParts(Token.TEXT).compareToIgnoreCase(b.showParts(Token.TEXT));
    }
}
