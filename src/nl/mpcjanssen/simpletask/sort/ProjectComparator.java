package nl.mpcjanssen.simpletask.sort;

import nl.mpcjanssen.simpletask.task.Task;

import java.util.Collections;
import java.util.List;

public class ProjectComparator extends ReversableComparator {
    public ProjectComparator(boolean reverse) {
        super(reverse);
    }

    @Override
    public int unreversedCompare(Task a, Task b) {
        List<String> projectsA = a.getProjects();
        List<String> projectsB = b.getProjects();

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
