package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.Nullable;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.token.Token;

public class AlphabeticalComparator extends Ordering<Task> {

    private final AlphabeticalStringComparator mStringComparator;

    public AlphabeticalComparator (boolean caseSensitive) {
        super();
        this.mStringComparator = new AlphabeticalStringComparator(caseSensitive);
    }

    @Override
    public int compare(@Nullable Task a, @Nullable Task b) {
        if (a==null) {
            a = new Task("");
        }
        if (b==null) {
            b = new Task("");
        }
        return mStringComparator.compare(a.showParts(Token.TEXT),b.showParts(Token.TEXT));
    }
}
