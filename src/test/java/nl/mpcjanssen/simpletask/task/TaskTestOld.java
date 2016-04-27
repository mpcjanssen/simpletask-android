package nl.mpcjanssen.simpletask.task;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.util.Util;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.TimeZone;

/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

public class TaskTestOld extends TestCase {

    public void testEquals() {
        Task a = new Task( "Test abcd");
        Task b = new Task( "Test abcd");
        Task c = new Task( "Test abcd ");
        Task d = new Task("Test abcd");
        assertFalse(b.equals(c));
        assertTrue(b.equals(d));
        assertTrue(a.equals(b));
    }
    

    public void testParseIdemPotence() {
        String s = "Test abcd ";
        String t = " ";
        String v = "";
        assertEquals(s,new Task(s).text);
        assertEquals(t,new Task(t).text);
        assertEquals(v,new Task(v).text);
    }

    public void testHidden() {
        assertTrue(new Task("Test h:1").isHidden());
        assertFalse(new Task("Test").isHidden());
        assertTrue(new Task("h:1").isHidden());
    }

    public void testCompletion() {
        String rawText = "Test";
        Task t = new Task( rawText);
        t.markComplete("2001-01-01");
        assertTrue(t.isCompleted());
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(rawText, t.text);
    }

    public void testCompletionWithPrependDate() {
        String rawText = "Test";
        Task t = new Task( rawText, "2000-10-10");
        rawText = t.text;
        DateTime completionDate = DateTime.today(TimeZone.getDefault());
        t.markComplete("2222-11-11");
        assertTrue(t.isCompleted());
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(rawText, t.text);

        t = new Task( "x 2000-01-01 2001-01-01 Test");
        assertEquals("2000-01-01", t.getCompletionDate());
        assertEquals("2001-01-01", t.getCreateDate());

        t = new Task( "x 2000-01-01 (A) 2001-01-01 Test");
        assertEquals("2000-01-01", t.getCompletionDate());
        assertEquals("2001-01-01", t.getCreateDate());
        assertEquals(Priority.A,t.getPriority());
    }

    public void testCompletionWithPriority1() {
        String rawText = "(A) Test";
        Task t = new Task( rawText);
        assertEquals(Priority.A, t.getPriority());

        DateTime completionDate = DateTime.today(TimeZone.getDefault());
        t.markComplete("2010-01-01");
        assertTrue(t.isCompleted());
        t.setPriority(Priority.B);
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(Priority.B , t.getPriority());
        assertEquals("(B) Test", t.text);
    }

    public void testCompletionWithPriority2() {
        String rawText = "(A) Test";
        Task t = new Task( rawText);
        t.update(rawText);
        assertEquals(t.getPriority(), Priority.A);
        DateTime completionDate = DateTime.today(TimeZone.getDefault());
        t.markComplete("2001-01-01");
        assertTrue(t.isCompleted());
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(Priority.A , t.getPriority());
        assertEquals("(A) Test", t.text);
    }
    public void testPriority() {
        Task t = new Task( "(C) Test");
        assertEquals(t.getPriority(), Priority.C);
        t.setPriority(Priority.A);
        assertEquals(t.getPriority(), Priority.A);
        t.setPriority(Priority.NONE);
        assertEquals(t.getPriority(), Priority.NONE);
        t = new Task( "Test");
        assertEquals(t.getPriority(), Priority.NONE);
        t.setPriority(Priority.A);
        assertEquals(t.getPriority(), Priority.A);
        assertEquals("(A) Test", t.text);
        t.setPriority(Priority.NONE);
        assertEquals(t.getPriority(), Priority.NONE);
        assertEquals("Test", t.text);
    }

    public void testCompletedPriority() {
        Task t = new Task("x 1111-11-11 (A) Test");
        assertTrue(t.isCompleted());
        assertEquals(Priority.A,t.getPriority());
    }

    public void testRemoveTag() {
        Task t = new Task( "Milk @@errands");
        t.removeList("errands");
        assertEquals("Milk @@errands", t.text);
        t.removeList("@errands");
        assertEquals("Milk", t.text);
        assertEquals("Milk", t.getText());
        t = new Task( "Milk @@errands +supermarket");
        t.removeList("@errands");
        assertEquals("Milk +supermarket", t.text);
    }

    public void testRecurrence() {
        Task t1 = new Task( "Test");
        Task t2 = new Task( "Test rec:1d");
        assertEquals(null, t1.getRecurrencePattern());
        assertEquals("1d", t2.getRecurrencePattern());
        String t3 = "(B) 2014-07-05 Test t:2014-07-05 rec:2d";
        String t3a = "(B) 2014-07-05 Test t:2014-07-05 rec:+2d";
        Task t4 = new Task(t3).markComplete("2000-01-01");
        Task t5 = new Task(t3a).markComplete("2000-01-01");
        assertNotNull(t4);
        assertNotNull(t5);
        assertEquals("(B) 2000-01-01 Test t:2000-01-03 rec:2d", t4.text);
        assertEquals("(B) 2000-01-01 Test t:2014-07-07 rec:+2d", t5.text);

        String dt3 = "(B) 2014-07-05 Test due:2014-07-05 rec:2d";
        String dt3a = "(B) 2014-07-05 Test due:2014-07-05 rec:+2d";
        Task dt4 = new Task(dt3).markComplete("2000-01-01");
        Task dt5 = new Task(dt3a).markComplete("2000-01-01");
        assertNotNull(dt4);
        assertNotNull(dt5);
        assertEquals("(B) 2000-01-01 Test due:2000-01-03 rec:2d", dt4.text);
        assertEquals("(B) 2000-01-01 Test due:2014-07-07 rec:+2d", dt5.text);

        String text = "Test due:2014-07-05 rec:1y";
        Task task = new Task(text).markComplete("2000-01-01");
        assertNotNull(task);
        assertEquals("Test due:2001-01-01 rec:1y", task.text);
    }

    public void testDue() {
        Task t1 = new Task("Test");
        t1.setDueDate("2013-01-01");
        assertEquals("Test due:2013-01-01", t1.text);
        // Don't add extra whitespace
        t1.setDueDate("2013-01-01");
        assertEquals("Test due:2013-01-01", t1.text);
        // Don't leave behind whitespace
        t1.setDueDate("");
        assertEquals("Test", t1.text);
    }

    public void testThreshold() {
        Task t1 = new Task( "t:2013-12-12 Test");
        Task t2 = new Task( "Test t:2013-12-12");

        assertEquals("2013-12-12", t1.getThresholdDate());
        assertEquals("2013-12-12", t2.getThresholdDate());
        Task t3 = new Task( "Test");
        assertNull(t3.getThresholdDate());
        t3.setThresholdDate("2013-12-12");
        assertEquals("Test t:2013-12-12", t3.text);
    }

    public void testInvalidThresholdDate() {
        Task t1 = new Task( "Test t:2013-11-31");
        assertFalse(t1.inFuture("2015-01-01"));
    }

    public void testInvalidDueDate() {
        Task t1 = new Task( "Test due:2013-11-31");
        assertEquals("2013-11-31",t1.getDueDate());
    }

    public void testInvalidCompleteDate() {
        Task t1 = new Task( "x 2013-11-31 Test");
        assertEquals("2013-11-31",t1.getCompletionDate());
    }

}
