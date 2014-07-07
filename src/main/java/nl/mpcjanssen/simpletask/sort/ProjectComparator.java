package nl.mpcjanssen.simpletask.sort;

import com.google.common.collect.Ordering;

import java.util.Collections;
import java.util.List;

import nl.mpcjanssen.simpletask.task.Task;

public class ProjectComparator extends Ordering<Task> {

    @Override
    public int compare(Task a, Task b) {
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
            return projectsA.get(0).compareToIgnoreCase(projectsB.get(0));
        }
    }
}
