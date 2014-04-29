package nl.mpcjanssen.simpletask.task;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import java.util.Date;

import nl.mpcjanssen.simpletask.task.Task;

/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

public class TaskTest extends TestCase {
    public void testValidTag() throws Exception {
       assertEquals(false, Task.validTag(" "));
    }

    public void testEquals() {
        Task a = new Task(1, "Test abcd");
        Task b = new Task(1, "Test abcd");
        Task c = new Task(1, "Test abcd ");
        Task d = new Task(2, "Test abcd");
        assertNotSame(a,b);
        assertEquals(a,b);
        assertFalse(b.equals(c));
        assertFalse(b.equals(d));
    }
    

    public void testHidden() {
        assertTrue(new Task(0,"Test h:1").isHidden());
        assertFalse(new Task(0,"Test").isHidden());
        assertTrue(new Task(0,"h:1").isHidden());
    }

    public void testCompletion() {
        String rawText = "Test";
        Task t = new Task(0, rawText);
        DateTime completionDate = new DateTime();
        t.markComplete(completionDate);
        assertTrue(t.isCompleted());
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(rawText, t.inFileFormat());
    }
    public void testCompletionWithPrependDate() {
        String rawText = "Test";
        Task t = new Task(0, rawText, new DateTime());
        rawText = t.inFileFormat();
        DateTime completionDate = new DateTime();
        t.markComplete(completionDate);
        assertTrue(t.isCompleted());
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(rawText, t.inFileFormat());
    }

    public void testCompletionWithPriority1() {
        String rawText = "(A) Test";
        Task t = new Task(0, rawText);
        t.update(rawText);
        assertEquals(t.getPriority(), Priority.A);
        DateTime completionDate = new DateTime();
        t.markComplete(completionDate);
        assertTrue(t.isCompleted());
        t.setPriority(Priority.B);
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(Priority.B , t.getPriority());
        assertEquals("(B) Test", t.inFileFormat());
    }

    public void testCompletionWithPriority2() {
        String rawText = "(A) Test";
        Task t = new Task(0, rawText);
        t.update(rawText);
        assertEquals(t.getPriority(), Priority.A);
        DateTime completionDate = new DateTime();
        t.markComplete(completionDate);
        assertTrue(t.isCompleted());
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(Priority.A , t.getPriority());
        assertEquals("(A) Test", t.inFileFormat());
    }
    public void testPriority() {
        Task t = new Task(0, "(C) Test");
        assertEquals(t.getPriority(), Priority.C);
        t.setPriority(Priority.A);
        assertEquals(t.getPriority(), Priority.A);
        t.setPriority(Priority.NONE);
        assertEquals(t.getPriority(), Priority.NONE);
        t = new Task(0, "Test");
        assertEquals(t.getPriority(), Priority.NONE);
        t.setPriority(Priority.A);
        assertEquals(t.getPriority(), Priority.A);
        assertEquals("(A) Test", t.inFileFormat());
        t.setPriority(Priority.NONE);
        assertEquals(t.getPriority(), Priority.NONE);
        assertEquals("Test", t.inFileFormat());
    }

    public void testCompletedPriority() {
        Task t = new Task(0,"x 1111-11-11 (A) Test bcd");
        assertTrue(t.isCompleted());
        assertEquals(Priority.A,t.getPriority());
    }

    public void testRemoveTag() {
        Task t = new Task(0, "Milk @@errands");
        t.removeTag("@errands");
        assertEquals("Milk @@errands", t.inFileFormat());
        t.removeTag("@@errands");
        assertEquals("Milk", t.inFileFormat());
        assertEquals("Milk", t.inScreenFormat(null));
        t = new Task(0, "Milk @@errands +supermarket");
        t.removeTag("@@errands");
        assertEquals("Milk +supermarket", t.inFileFormat());
    }

    public void testRecurrence() {
        Task t1 = new Task(0, "Test");
        Task t2 = new Task(0, "Test rec:1d");
        assertEquals(null, t1.getRecurrencePattern());
        assertEquals("1d", t2.getRecurrencePattern());
    }

    public void testThreshold() {
        Task t1 = new Task(0, "t:2013-12-12 Test");
        Task t2 = new Task(0, "Test t:2013-12-12");
        assertEquals("2013-12-12", t1.getThresholdDateString(""));
        assertEquals("2013-12-12", t2.getThresholdDateString(""));
    }

    public void testInvalidThresholdDate() {
        Task t1 = new Task(0, "Test t:2013-11-31");
        assertFalse(t1.inFuture());
    }

    public void testInvalidDueDate() {
        Task t1 = new Task(0, "Test due:2013-11-31");
        assertEquals(null,t1.getDueDate());
    }

    public void testInvalidCreateDate() {
        Task t1 = new Task(0, "2013-11-31 Test");
        assertEquals("2013-11-31",t1.getRelativeAge());
    }

    public void testInvalidCompleteDate() {
        Task t1 = new Task(0, "x 2013-11-31 Test");
        assertEquals("2013-11-31",t1.getCompletionDate());
    }
}
