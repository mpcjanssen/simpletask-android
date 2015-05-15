package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import nl.mpcjanssen.simpletask.task.Task;

public class ProjectComparator extends Ordering<Task> {

    private final AlphabeticalStringComparator mStringComparator;

    public ProjectComparator (boolean caseSensitive) {
        super();
        this.mStringComparator = new AlphabeticalStringComparator(caseSensitive);
    }


    @Override
    public int compare(@NotNull Task a, @NotNull Task b) {
        List<String> projectsA = a.getTags();
        List<String> projectsB = b.getTags();

        if (projectsA.isEmpty() && projectsB.isEmpty()) {
            return 0;
        } else if (projectsA.isEmpty() && !projectsB.isEmpty()) {
            return 1;
        } else if (!projectsA.isEmpty() && projectsB.isEmpty()) {
            return -1;
        } else {
            Collections.sort(projectsA);
            Collections.sort(projectsB);
            return mStringComparator.compare(projectsA.get(0),projectsB.get(0));
        }
    }
}
