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
        if (Strings.isEmptyOrNull(a.getCreateDate()) && Strings.isEmptyOrNull(b.getCreateDate())) {
            result = 0;
        } else if (Strings.isEmptyOrNull(a.getCreateDate())) {
            result = 1;
        } else if (Strings.isEmptyOrNull(b.getCreateDate())) {
            result = -1;
        } else {
            DateTime dateA = new DateTime(a.getCreateDate());
            DateTime dateB = new DateTime(b.getCreateDate());
            result = dateA.compareTo(dateB);
        }
        return result;
    }
}
