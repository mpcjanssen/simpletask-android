package nl.mpcjanssen.simpletask.sort;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;

import nl.mpcjanssen.simpletask.sort.CompletedComparator;
import nl.mpcjanssen.simpletask.sort.CreationDateComparator;
import nl.mpcjanssen.simpletask.sort.DueDateComparator;
import nl.mpcjanssen.simpletask.task.Task;

public class SortTest extends TestCase {
    public void testAlphabeticalSort1 () {
        ArrayList<Task> tasks = new ArrayList<Task>();
        Task t1 = new Task("2011-01-01 B");
        Task t2 = new Task("2012-01-01 A");
        tasks.add(t1);
        tasks.add(t2);
        assertSame(t1, tasks.get(0));
        Collections.sort(tasks, new AlphabeticalComparator(true));
        assertSame(t2, tasks.get(0));
    }

    public void testAlphabeticalSort2 () {
        ArrayList<Task> tasks = new ArrayList<Task>();
        Task t1 = new Task("(A) B");
        Task t2 = new Task("(B) A");
        tasks.add(t1);
        tasks.add(t2);
        assertSame(t1, tasks.get(0));
        Collections.sort(tasks, new AlphabeticalComparator(true));
        assertSame(t2, tasks.get(0));
    }
}
