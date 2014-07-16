package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.Nullable;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.token.Token;

public class AlphabeticalComparator extends Ordering<Task> {
    @Override
    public int compare(@Nullable Task a, @Nullable Task b) {
        if (a==null) {
            a = new Task(0,"");
        }
        if (b==null) {
            b = new Task(0,"");
        }
        return a.showParts(Token.TEXT).compareToIgnoreCase(b.showParts(Token.TEXT));
    }
}
