package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.Nullable;


public class AlphabeticalStringComparator extends Ordering<String> {
    @Override
    public int compare(@Nullable String a, @Nullable String b) {
        if (a==null) {
            a = "";
        }
        if (b==null) {
            b = "";
        }
        return a.compareToIgnoreCase(b);
    }
}
