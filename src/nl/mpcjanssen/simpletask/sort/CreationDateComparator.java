package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Strings;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

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

            DateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT);
            // a and b are both not null

            try {
                Date dateA = formatter.parse(a.getPrependedDate());
                Date dateB = formatter.parse(b.getPrependedDate());
                result = dateA.compareTo(dateB);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
