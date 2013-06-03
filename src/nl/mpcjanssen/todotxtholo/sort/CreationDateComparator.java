package nl.mpcjanssen.todotxtholo.sort;

import nl.mpcjanssen.todotxtholo.Constants;
import nl.mpcjanssen.todotxtholo.task.Task;
import nl.mpcjanssen.todotxtholo.util.Strings;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;

public class CreationDateComparator implements Comparator<Task> {
    @Override
    public int compare(Task b, Task a) {
        // TODO Task b, Task a to get newest first. Replace with multisort with reverse
        if (Strings.isEmptyOrNull(a.getPrependedDate()) && Strings.isEmptyOrNull(b.getPrependedDate())) {
            return 0;
        } else if (Strings.isEmptyOrNull(a.getPrependedDate())) {
            return 1;
        } else if (Strings.isEmptyOrNull(b.getPrependedDate())) {
            return -1;
        }

        DateFormat formatter = new SimpleDateFormat(Constants.DATE_FORMAT);
        // a and b are both not null

        try {
            Date dateA = formatter.parse(a.getPrependedDate());
            Date dateB = formatter.parse(b.getPrependedDate());
            return dateA.compareTo(dateB);
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
