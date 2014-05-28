package nl.mpcjanssen.simpletask.sort;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Strings;

public class CreationDateComparator extends ReversableComparator {

    public CreationDateComparator(boolean b) {
        super(b);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        int result = 0;
        if (Strings.isEmptyOrNull(a.getPrependedDate()) && Strings.isEmptyOrNull(b.getPrependedDate())) {
            result = 0;
        } else if (Strings.isEmptyOrNull(a.getPrependedDate())) {
            result = 1;
        } else if (Strings.isEmptyOrNull(b.getPrependedDate())) {
            result = -1;
        } else {
            DateTime dateA = new DateTime(a.getPrependedDate());
            DateTime dateB = new DateTime(b.getPrependedDate());
            result = dateA.compareTo(dateB);
        }
        return result;
    }
}
