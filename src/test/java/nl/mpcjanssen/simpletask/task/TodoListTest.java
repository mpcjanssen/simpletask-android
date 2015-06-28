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

    public void testCompletedPrio () {
        TodoList todoList = new TodoList(null);
        Task t1 = new Task("(A) Test");
        Task t2 = new Task("(B) Test");
        todoList.add(t1);
        todoList.add(t2);
        todoList.complete(t1, false, true);
        todoList.complete(t2, false, false);
        assertEquals(Priority.A, t1.getPriority());
        assertEquals(Priority.NONE, t2.getPriority());
    }
}
