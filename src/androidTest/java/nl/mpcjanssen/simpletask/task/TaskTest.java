package nl.mpcjanssen.simpletask.task;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.token.*;

/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

public class TaskTest extends TestCase {

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
    

    public void testParseIdemPotence() {
        String s = "Test abcd ";
        assertEquals(s,new Task(0,s).inFileFormat());
    }

    public void testWithoutCompletionInfo() {
        Task t = new Task(0, "(B) 2014-07-05 Test t:2014-07-05 rec:2d");
        assertEquals("(B) Test t:2014-07-05 rec:2d", t.getTextWithoutCompletionInfo());
    }

    public void testHidden() {
        assertTrue(!new Task(0,"Test h:1").isVisible());
        assertFalse(!new Task(0,"Test").isVisible());
        assertTrue(!new Task(0,"h:1").isVisible());
    }

    public void testCompletion() {
        String rawText = "Test";
        Task t = new Task(0, rawText);
        DateTime completionDate = DateTime.today(TimeZone.getDefault());
        t.markComplete(completionDate, true);
        assertTrue(t.isCompleted());
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(rawText, t.inFileFormat());
    }
    public void testCompletionWithPrependDate() {
        String rawText = "Test";
        Task t = new Task(0, rawText, DateTime.today(TimeZone.getDefault()));
        rawText = t.inFileFormat();
        DateTime completionDate = DateTime.today(TimeZone.getDefault());
        t.markComplete(completionDate, true);
        assertTrue(t.isCompleted());
        t.markIncomplete();
        assertFalse(t.isCompleted());
        assertEquals(rawText, t.inFileFormat());
    }

    public void testCompletionWithPriority1() {
        String rawText = "(A) Test";
        Task t = new Task(0, rawText);
        assertEquals(Priority.A, t.getPriority());
        ArrayList<Token> expectedTokens = new ArrayList<Token>();
        expectedTokens.add(new PRIO("(A) "));
        expectedTokens.add(new TEXT("Test"));
        assertEquals(expectedTokens, t.getTokens());
        DateTime completionDate = DateTime.today(TimeZone.getDefault());
        t.markComplete(completionDate, true);
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
        DateTime completionDate = DateTime.today(TimeZone.getDefault());
        t.markComplete(completionDate, true);
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
        Task t = new Task(0,"x 1111-11-11 (A) Test");
        ArrayList<Token> expectedTokens = new ArrayList<Token>();
        expectedTokens.add(new COMPLETED());
        expectedTokens.add(new COMPLETED_DATE("1111-11-11 "));
        expectedTokens.add(new PRIO("(A) "));
        expectedTokens.add(new TEXT("Test"));
        assertEquals(expectedTokens, t.getTokens());
        assertTrue(t.isCompleted());
        assertEquals(Priority.A,t.getPriority());
    }

    public void testRemoveTag() {
        Task t = new Task(0, "Milk @@errands");
        t.removeTag("@errands");
        assertEquals("Milk @@errands", t.inFileFormat());
        t.removeTag("@@errands");
        assertEquals("Milk", t.inFileFormat());
        assertEquals("Milk", t.showParts(Token.SHOW_ALL));
        t = new Task(0, "Milk @@errands +supermarket");
        t.removeTag("@@errands");
        assertEquals("Milk +supermarket", t.inFileFormat());
    }

    public void testRecurrence() {
        Task t1 = new Task(0, "Test");
        Task t2 = new Task(0, "Test rec:1d");
        assertEquals(null, t1.getRecurrencePattern());
        assertEquals("1d", t2.getRecurrencePattern());
        String t3 = "(B) 2014-07-05 Test t:2014-07-05 rec:2d";
        Task t4 = new Task(0,t3).markComplete(DateTime.forDateOnly(2000,01,01), true);
        Task t5 = new Task(0,t3).markComplete(DateTime.forDateOnly(2000,01,01), false);
        assertEquals("(B) 2000-01-01 Test t:2014-07-07 rec:2d", t4.inFileFormat());
        assertEquals("(B) 2000-01-01 Test t:2000-01-03 rec:2d", t5.inFileFormat());
    }

    public void testDue() {
        Task t1 = new Task(0,"Test");
        t1.setDueDate("2013-01-01");
        assertEquals("Test due:2013-01-01", t1.inFileFormat());
        // Don't add extra whitespace
        t1.setDueDate("2013-01-01");
        assertEquals("Test due:2013-01-01", t1.inFileFormat());
        // Don't leave behind whitespace
        t1.setDueDate("");
        assertEquals("Test", t1.inFileFormat());
    }

    public void testThreshold() {
        Task t1 = new Task(0, "t:2013-12-12 Test");
        Task t2 = new Task(0, "Test t:2013-12-12");
        ArrayList<Token> eTok = new ArrayList<Token>();
        eTok.add(new THRESHOLD_DATE("2013-12-12"));
        eTok.add(new WHITE_SPACE(" "));
        eTok.add(new TEXT("Test"));
        assertEquals(eTok, t1.getTokens());
        assertEquals("2013-12-12", t1.getThresholdDateString(""));
        assertEquals("2013-12-12", t2.getThresholdDateString(""));
        Task t3 = new Task(0, "Test");
        assertNull(t3.getThresholdDate());
        t3.setThresholdDate("2013-12-12");
        assertEquals("Test t:2013-12-12", t3.inFileFormat());
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

    public void testParseText() {
        Task t1 = new Task(0, "abcd");
        ArrayList<Token> expected = new ArrayList<Token>();
        expected.add(new TEXT("abcd"));
        assertEquals(expected, t1.getTokens());
    }
}
