package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.Nullable;


public class AlphabeticalStringComparator extends Ordering<String> {
    boolean bCaseSensitive = true;

    public AlphabeticalStringComparator(boolean caseSensitive) {
        super();
        this.bCaseSensitive = caseSensitive;
    }

    @Override
    public int compare(@Nullable String a, @Nullable String b) {
        if (a==null) {
            a = "";
        }
        if (b==null) {
            b = "";
        }
        if (bCaseSensitive) {
            return a.compareTo(b);
        } else {
            return a.compareToIgnoreCase(b);
        }
    }
}
