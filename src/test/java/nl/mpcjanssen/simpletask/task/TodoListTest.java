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
import org.junit.Test;


public class TodoListTest extends TestCase {
    @Test
    public void testRecurrence () {
        TodoList todoList = new TodoList(null);
        Task t = new Task("Test rec:1d");
        todoList.add(t);
        assertEquals(1, todoList.size());
        todoList.complete(t, false, false);
        assertEquals(2, todoList.size());
    }
}
