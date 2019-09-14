package nl.mpcjanssen.simpletask.task

import junit.framework.TestCase

/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

class TaskTestOld : TestCase() {

    fun testEquals() {
        val a = Task("Test abcd")
        val b = Task("Test abcd")
        val c = Task("Test abcd ")
        val d = Task("Test abcd")
        TestCase.assertFalse(b == c)
        TestCase.assertTrue(d == d)
        TestCase.assertFalse(a == b)
    }


    fun testParseIdemPotence() {
        val s = "Test abcd "
        val t = " "
        val v = ""
        TestCase.assertEquals(s, Task(s).inFileFormat())
        TestCase.assertEquals(t, Task(t).inFileFormat())
        TestCase.assertEquals(v, Task(v).inFileFormat())
    }

    fun testHidden() {
        TestCase.assertTrue(Task("Test h:1").isHidden())
        TestCase.assertFalse(Task("Test").isHidden())
        TestCase.assertTrue(Task("h:1").isHidden())
    }

    fun testCompletion() {
        val rawText = "Test"
        val t = Task(rawText)
        t.markComplete("2001-01-01")
        TestCase.assertTrue(t.isCompleted())
        t.markIncomplete()
        TestCase.assertFalse(t.isCompleted())
        TestCase.assertEquals(rawText, t.inFileFormat())
    }

    fun testCompletionWithPrependDate() {
        var rawText = "Test"
        var t = Task(rawText, "2000-10-10")
        rawText = t.inFileFormat()
        t.markComplete("2222-11-11")
        TestCase.assertTrue(t.isCompleted())
        t.markIncomplete()
        TestCase.assertFalse(t.isCompleted())
        TestCase.assertEquals(rawText, t.inFileFormat())

        t = Task("x 2000-01-01 2001-01-01 Test")
        TestCase.assertEquals("2000-01-01", t.completionDate)
        TestCase.assertEquals("2001-01-01", t.createDate)

        t = Task("x 2000-01-01 (A) 2001-01-01 Test")
        TestCase.assertEquals("2000-01-01", t.completionDate)
        TestCase.assertEquals("2001-01-01", t.createDate)
        TestCase.assertEquals(Priority.A, t.priority)
    }

    fun testCompletionWithPriority1() {
        val rawText = "(A) Test"
        val t = Task(rawText)
        TestCase.assertEquals(Priority.A, t.priority)

        t.markComplete("2010-01-01")
        TestCase.assertTrue(t.isCompleted())
        t.priority = Priority.B
        t.markIncomplete()
        TestCase.assertFalse(t.isCompleted())
        TestCase.assertEquals(Priority.B, t.priority)
        TestCase.assertEquals("(B) Test", t.inFileFormat())
    }

    fun testCompletionWithPriority2() {
        val rawText = "(A) Test"
        val t = Task(rawText)
        t.update(rawText)
        TestCase.assertEquals(t.priority, Priority.A)
        t.markComplete("2001-01-01")
        TestCase.assertTrue(t.isCompleted())
        t.markIncomplete()
        TestCase.assertFalse(t.isCompleted())
        TestCase.assertEquals(Priority.A, t.priority)
        TestCase.assertEquals("(A) Test", t.inFileFormat())
    }

    fun testPriority() {
        var t = Task("(C) Test")
        TestCase.assertEquals(t.priority, Priority.C)
        t.priority = Priority.A
        TestCase.assertEquals(t.priority, Priority.A)
        t.priority = Priority.NONE
        TestCase.assertEquals(t.priority, Priority.NONE)
        t = Task("Test")
        TestCase.assertEquals(t.priority, Priority.NONE)
        t.priority = Priority.A
        TestCase.assertEquals(t.priority, Priority.A)
        TestCase.assertEquals("(A) Test", t.inFileFormat())
        t.priority = Priority.NONE
        TestCase.assertEquals(t.priority, Priority.NONE)
        TestCase.assertEquals("Test", t.inFileFormat())
    }

    fun testCompletedPriority() {
        val t = Task("x 1111-11-11 (A) Test")
        TestCase.assertTrue(t.isCompleted())
        TestCase.assertEquals(Priority.A, t.priority)
    }

    fun testRemoveTag() {
        var t = Task("Milk @@errands")
        t.removeList("errands")
        TestCase.assertEquals("Milk @@errands", t.inFileFormat())
        t.removeList("@errands")
        TestCase.assertEquals("Milk", t.inFileFormat())
        TestCase.assertEquals("Milk", t.text)
        t = Task("Milk @@errands +supermarket")
        t.removeList("@errands")
        TestCase.assertEquals("Milk +supermarket", t.inFileFormat())
    }

    fun testRecurrence() {
        val t1 = Task("Test")
        val t2 = Task("Test rec:1d")
        TestCase.assertEquals(null, t1.recurrencePattern)
        TestCase.assertEquals("1d", t2.recurrencePattern)
        val t3 = "(B) 2014-07-05 Test t:2014-07-05 rec:2d"
        val t3a = "(B) 2014-07-05 Test t:2014-07-05 rec:+2d"
        val t4 = Task(t3).markComplete("2000-01-01")
        val t5 = Task(t3a).markComplete("2000-01-01")
        TestCase.assertNotNull(t4)
        TestCase.assertNotNull(t5)
        TestCase.assertEquals("(B) 2000-01-01 Test t:2000-01-03 rec:2d", t4!!.inFileFormat())
        TestCase.assertEquals("(B) 2000-01-01 Test t:2014-07-07 rec:+2d", t5!!.inFileFormat())

        val dt3 = "(B) 2014-07-05 Test due:2014-07-05 rec:2d"
        val dt3a = "(B) 2014-07-05 Test due:2014-07-05 rec:+2d"
        val dt4 = Task(dt3).markComplete("2000-01-01")
        val dt5 = Task(dt3a).markComplete("2000-01-01")
        TestCase.assertNotNull(dt4)
        TestCase.assertNotNull(dt5)
        TestCase.assertEquals("(B) 2000-01-01 Test due:2000-01-03 rec:2d", dt4!!.inFileFormat())
        TestCase.assertEquals("(B) 2000-01-01 Test due:2014-07-07 rec:+2d", dt5!!.inFileFormat())

        val text = "Test due:2014-07-05 rec:1y"
        val task = Task(text).markComplete("2000-01-01")
        TestCase.assertNotNull(task)
        TestCase.assertEquals("Test due:2001-01-01 rec:1y", task!!.inFileFormat())
    }

    fun testDue() {
        val t1 = Task("Test")
        t1.dueDate = "2013-01-01"
        TestCase.assertEquals("Test due:2013-01-01", t1.inFileFormat())
        // Don't add extra whitespace
        t1.dueDate = "2013-01-01"
        TestCase.assertEquals("Test due:2013-01-01", t1.inFileFormat())
        // Don't leave behind whitespace
        t1.dueDate = ""
        TestCase.assertEquals("Test", t1.inFileFormat())
    }

    fun testThreshold() {
        val t1 = Task("t:2013-12-12 Test")
        val t2 = Task("Test t:2013-12-12")

        TestCase.assertEquals("2013-12-12", t1.thresholdDate)
        TestCase.assertEquals("2013-12-12", t2.thresholdDate)
        val t3 = Task("Test")
        TestCase.assertNull(t3.thresholdDate)
        t3.thresholdDate = "2013-12-12"
        TestCase.assertEquals("Test t:2013-12-12", t3.inFileFormat())
    }

    fun testInvalidThresholdDate() {
        val t1 = Task("Test t:2013-11-31")
        TestCase.assertFalse(t1.inFuture("2015-01-01", false))
    }

    fun testInvalidDueDate() {
        val t1 = Task("Test due:2013-11-31")
        TestCase.assertEquals("2013-11-31", t1.dueDate)
    }

    fun testInvalidCompleteDate() {
        val t1 = Task("x 2013-11-31 Test")
        TestCase.assertEquals("2013-11-31", t1.completionDate)
    }

}
