package nl.mpcjanssen.simpletask.util;

import junit.framework.TestCase;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoListItem;

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
        ArrayList<TodoListItem> items = new ArrayList<>();
        for (Task t : tasks) {
            items.add(new TodoListItem(0,t,false));
        }
        assertEquals(0,Util.addHeaderLines(items, "by_context", "none", false, false).size());

    }
}
