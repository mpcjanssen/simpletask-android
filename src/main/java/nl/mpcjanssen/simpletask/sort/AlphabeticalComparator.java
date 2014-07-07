package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.token.Token;

public class AlphabeticalComparator extends Ordering<Task> {
    @Override
    public int compare(Task a, Task b) {
        return a.showParts(Token.TEXT).compareToIgnoreCase(b.showParts(Token.TEXT));
    }
}
