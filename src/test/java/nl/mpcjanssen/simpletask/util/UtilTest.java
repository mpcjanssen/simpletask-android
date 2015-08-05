package nl.mpcjanssen.simpletask.util;

import hirondelle.date4j.DateTime;
import junit.framework.TestCase;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Util;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

public class UtilTest extends TestCase {
    public void testAddHeaderLines () {
        ArrayList<Task> tasks = new ArrayList<>();
        tasks.add(new Task("@Home h:1"));
        tasks.add(new Task("@Work h:1"));
        tasks.add(new Task("@College h:1"));
        assertEquals(0,Util.addHeaderLines(tasks, "by_context", "none", false, false).size());

    }
}
