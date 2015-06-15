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
        Task t1 = new Task(0,"2011-01-01 B");
        Task t2 = new Task(0,"2012-01-01 A");
        tasks.add(t1);
        tasks.add(t2);
        assertSame(t1, tasks.get(0));
        Collections.sort(tasks, new AlphabeticalComparator(true));
        assertSame(t2, tasks.get(0));
    }

    public void testAlphabeticalSort2 () {
        ArrayList<Task> tasks = new ArrayList<Task>();
        Task t1 = new Task(0,"(A) B");
        Task t2 = new Task(0,"(B) A");
        tasks.add(t1);
        tasks.add(t2);
        assertSame(t1, tasks.get(0));
        Collections.sort(tasks, new AlphabeticalComparator(true));
        assertSame(t2, tasks.get(0));
    }

    public void testCompletedComparator() {
        ArrayList<Task> tasks = new ArrayList<Task>();
        tasks.add(null);
        tasks.add(null);
        tasks.add(new Task(0,""));
        tasks.add(new Task(0,""));
        Collections.sort(tasks, new CompletedComparator());
       tasks = new ArrayList<Task>();
        tasks.add(new Task(0,"a"));
        tasks.add(new Task(0,"b"));
        tasks.add(new Task(0,"c"));
        tasks.add(new Task(0,"d"));
        tasks.add(new Task(0,"e"));
        Collections.sort(tasks, new CreationDateComparator());
        Collections.sort(tasks, new DueDateComparator());
        Collections.sort(tasks, new FileOrderComparator());
    }

    public void testCreatedDateComparator() {
        ArrayList<Task> tasks = new ArrayList<Task>();
        tasks.add(new Task(0,""));
        tasks.add(new Task(0,""));
        Collections.sort(tasks, new CreationDateComparator());
    }
}
