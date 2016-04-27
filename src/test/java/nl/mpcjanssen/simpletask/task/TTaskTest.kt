package nl.mpcjanssen.simpletask.task

import junit.framework.TestCase

class TaskTest : TestCase() {
    fun testParseIdempotence() {
        var t = "x 1231 2 123 312 +test"
        assertEquals(t, Task(t).text)
        t = "Test abcd "
        assertEquals(t, Task(t).text)
        t = "x 2012-14-11 rec:12w mail@example.com"
        assertEquals(t, Task(t).text)
        t = "  2012-14-11 rec:12w mail@example.com  "
        assertEquals(t, Task(t).text)

        t = "(B) 2012-14-11 rec:12w mail@example.com  "
        assertEquals(t, Task(t).text)
    }

    fun testLexing() {
        assertEquals(listOf("ab", "b", "", "d", "s"), "ab b  d s".lex())
    }

    fun testEquals() {
        assertTrue(Task("a")==Task("a"))
        assertFalse(Task("a")==Task("A"))
    }

    fun testGetDueDate() {
        var t = "x 2012-14-11 due:2014-10-10 rec:12w mail@example.com"
        assertEquals("2014-10-10", Task(t).dueDate)
        t = "x 2012-14-11 due:2014-99-99 rec:12w mail@example.com"
        assertEquals("2014-99-99", Task(t).dueDate)
        t = "x 2012-14-11 rec:12w mail@example.com"
        assertEquals(null, Task(t).dueDate)
    }

    fun testThresholdDate() {
        var t = "x 2012-14-11 t:2014-10-10 rec:12w mail@example.com"
        assertEquals("2014-10-10", Task(t).thresholdDate)
        t = "x 2012-14-11 t:2014-99-99 rec:12w mail@example.com"
        assertEquals("2014-99-99", Task(t).thresholdDate)
        t = "x 2012-14-11 rec:12w mail@example.com"
        assertEquals(null, Task(t).thresholdDate)
        val task = Task("x t:2011-01-01")
        assertEquals("2011-01-01", task.thresholdDate)
        //task.thresholdDate = null
        assertEquals(null, task.thresholdDate)
        assertEquals("x", task.text)
        //task.thresholdDate = "2000-01-01"
        assertEquals("x t:2000-01-01", task.text)
    }

    fun testCreateDate() {
        val t1 = Task("2014-01-01 test")
        assertEquals("2014-01-01", t1.createDate)
        val t2 = Task("2014-01-01 test", "2010-01-01")
        assertEquals("2014-01-01", t2.createDate)
        val t3 = Task("test", "2010-01-01")
        assertEquals("2010-01-01", t3.createDate)
        assertEquals("2010-01-01 test", t3.text)
    }

    fun testGetCompletionDate() {
        var t = "x 2012-11-11 due:2014-10-10 rec:12w mail@example.com"
        assertEquals("2012-11-11", Task(t).completionDate)
    }

    fun testGetCreatedDate() {
        var t = "x 2012-11-11 due:2014-10-10 rec:12w mail@example.com"
        assertNull(Task(t).createDate)
        t = "x 2012-11-11 2013-11-11 due:2014-10-10 rec:12w mail@example.com"
        assertEquals("2013-11-11", Task(t).createDate)
        t = "2013-11-11 due:2014-10-10 rec:12w mail@example.com"
        assertEquals("2013-11-11", Task(t).createDate)
    }

/*    fun testShow() {
        var t = Task("x 2012-11-11 due:2014-10-10 morgen rec:12w mail@example.com")

        val text = t.tokens.filter {
            it.type == TEXT
        }.map{it.text}.joinToString(" ")
        assertEquals("morgen", text)
    }*/

    fun testCompletion() {
        var str = "test"
        val t1 =  Task(str)
        t1.markComplete("2010-12-12")
        assertEquals("x 2010-12-12 $str", t1.text )
        val tc =  Task("x 2000-12-12 $str")
        tc.markComplete("2010-12-12")
        assertEquals("x 2000-12-12 $str", tc.text )
    }
}