package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;
import nl.mpcjanssen.simpletask.task.Task;

import java.util.Collections;
import java.util.List;

public class ProjectComparator extends Ordering<Task> {

    private final AlphabeticalStringComparator mStringComparator;

    public ProjectComparator (boolean caseSensitive) {
        super();
        this.mStringComparator = new AlphabeticalStringComparator(caseSensitive);
    }


    @Override
    public int compare(Task a, Task b) {
        if (a == b) {
            return 0;
        } else if (a == null) {
            return -1;
        } else if (b == null) {
            return 1;
        }
        List<String> projectsA = a.getTags();
        List<String> projectsB = b.getTags();

        if (projectsA.isEmpty() && projectsB.isEmpty()) {
            return 0;
        } else if (projectsA.isEmpty()) {
            return 1;
        } else if (projectsB.isEmpty()) {
            return -1;
        } else {
            Collections.sort(projectsA);
            Collections.sort(projectsB);
            return mStringComparator.compare(projectsA.get(0),projectsB.get(0));
        }
    }
}
