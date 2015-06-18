package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Strings;

public class CreationDateComparator extends Ordering<Task> {

    @Override
    public int compare(Task a, Task b) {
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
