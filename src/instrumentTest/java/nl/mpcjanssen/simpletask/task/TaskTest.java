package nl.mpcjanssen.simpletask.task;

import android.test.AndroidTestCase;

/**
 * Created by A156712 on 20-7-13.
 */
public class TaskTest extends AndroidTestCase {
    public void testValidTag() throws Exception {
        assertEquals(false, Task.validTag(" "));
        assertEquals(true, Task.validTag("Abc"));

    }

    public void testIdentity() throws Exception {
        Task a = new Task(1,"Test");
        Task b = new Task(1,"Test");
        assertNotSame(a,b);
        assertEquals(a,b);
    }
}
