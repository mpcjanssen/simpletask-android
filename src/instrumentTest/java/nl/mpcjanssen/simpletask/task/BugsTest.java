package nl.mpcjanssen.simpletask.task;

import junit.framework.TestCase;

import nl.mpcjanssen.simpletask.task.Task;

/**
 * Created by Mark Janssen on 22-7-13.
 */
public class BugsTest extends TestCase {

    public void testBuga7248d85e () {
        // Even though tasks below is not technically formatted
        // correctly it should not be changed in the file
        String taskContents = "x test";
        Task t = new Task(0, taskContents);
        assertEquals(taskContents, t.inFileFormat());
    }
}