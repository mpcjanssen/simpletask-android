package nl.mpcjanssen.simpletask.test;

import junit.framework.TestCase;
import nl.mpcjanssen.simpletask.sort.PriorityComparator;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;

/**
 * Created with IntelliJ IDEA.
 * User: A156712
 * Date: 15-1-13
 * Time: 17:20
 * To change this template use File | Settings | File Templates.
 */
public class PriorityComparatorTest extends TestCase {
    PriorityComparator comp = new PriorityComparator();

    public void testCompareSamePrio() throws Exception {
        Task a = new Task(1, "");
        Task b = new Task(2, "");
        a.setPriority(Priority.A);
        b.setPriority(Priority.A);
        assertEquals(0, comp.compare(a, b));
    }

    public void testCompareNoPrio() throws Exception {
        Task a = new Task(1, "");
        Task b = new Task(2, "");
        a.setPriority(Priority.NONE);
        b.setPriority(Priority.NONE);
        assertEquals(0, comp.compare(a, b));
    }

    public void testCompareOnePrio() throws Exception {
        Task a = new Task(1, "");
        Task b = new Task(2, "");
        a.setPriority(Priority.A);
        b.setPriority(Priority.NONE);
        assertEquals(-1, comp.compare(a, b));
        assertEquals(1, comp.compare(b, a));
    }

    public void testCompareDifferentPrio() throws Exception {
        Task a = new Task(1, "");
        Task b = new Task(2, "");
        a.setPriority(Priority.A);
        b.setPriority(Priority.B);
        assertEquals(-1, comp.compare(a, b));
        assertEquals(1, comp.compare(b, a));
    }
}
