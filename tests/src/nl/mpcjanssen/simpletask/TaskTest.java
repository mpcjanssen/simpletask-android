package nl.mpcjanssen.simpletask;

import junit.framework.TestCase;
import nl.mpcjanssen.simpletask.task.Task;

/**
 * Created with IntelliJ IDEA.
 * User: A156712
 * Date: 21-7-13
 * Time: 12:28
 * To change this template use File | Settings | File Templates.
 */
public class TaskTest extends TestCase {
    public void testValidTag() throws Exception {
       assertEquals(false, Task.validTag(" "));
    }

    public void testEquals() throws Exception {
        Task a = new Task(1, "Test");
        Task b = new Task(1, "Test");
        assertNotSame(a,b);
        assertEquals(a,b);
    }
}
