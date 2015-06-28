package nl.mpcjanssen.simpletask.task;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;

import nl.mpcjanssen.simpletask.sort.CompletedComparator;
import nl.mpcjanssen.simpletask.sort.CreationDateComparator;
import nl.mpcjanssen.simpletask.sort.DueDateComparator;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;

public class TodoListTest extends TestCase {
    public void testRecurrence () {
        TodoList tl = new TodoList(null);
        Task t = new Task("Test rec:1d");
        tl.add(t);
        assertEquals(1, tl.size());
        t.markComplete(DateTime.now(TimeZone.getDefault()), false);
        assertEquals(2, tl.size());
    }
}
