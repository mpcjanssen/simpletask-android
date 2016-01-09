package nl.mpcjanssen.simpletask.task

import junit.framework.TestCase

class TTaskTest : TestCase() {
    fun testParseIdempotence() {
        var t = "x 1231 2 123 312 +test"
        assertEquals(t, TTask(t).text)
        t = "Test abcd "
        assertEquals(t, TTask(t).text)
        t = "x 2012-14-11 rec:12w mail@example.com"
        assertEquals(t, TTask(t).text)
        t = "  2012-14-11 rec:12w mail@example.com  "
        assertEquals(t, TTask(t).text)
    }

    fun testLexing() {
        assertEquals(listOf("ab", "b", "", "d", "s"), "ab b  d s".lex())
    }

    fun testEquals() {
        assertTrue(TTask("a")==TTask("a"))
        assertFalse(TTask("a")==TTask("A"))
    }

    fun testGetDueDate() {
        var t = "x 2012-14-11 due:2014-10-10 rec:12w mail@example.com"
        assertEquals("2014-10-10", TTask(t).dueDate)
        t = "x 2012-14-11 due:2014-99-99 rec:12w mail@example.com"
        assertEquals("2014-99-99", TTask(t).dueDate)
        t = "x 2012-14-11 rec:12w mail@example.com"
        assertEquals(null, TTask(t).dueDate)
    }

    fun testThresholdDate() {
        var t = "x 2012-14-11 t:2014-10-10 rec:12w mail@example.com"
        assertEquals("2014-10-10", TTask(t).thresholdDate)
        t = "x 2012-14-11 t:2014-99-99 rec:12w mail@example.com"
        assertEquals("2014-99-99", TTask(t).thresholdDate)
        t = "x 2012-14-11 rec:12w mail@example.com"
        assertEquals(null, TTask(t).thresholdDate)
        val task = TTask("x t:2011-01-01")
        assertEquals("2011-01-01", task.thresholdDate)
        task.thresholdDate = null
        assertEquals(null, task.thresholdDate)
        assertEquals("x", task.text)
        task.thresholdDate = "2000-01-01"
        assertEquals("x t:2000-01-01", task.text)
    }

    fun testCreateDate() {
        val t1 = TTask("2014-01-01 test")
        assertEquals("2014-01-01", t1.createdDate)
        val t2 = TTask("2014-01-01 test", "2010-01-01")
        assertEquals("2014-01-01", t2.createdDate)
        val t3 = TTask("test", "2010-01-01")
        assertEquals("2010-01-01", t3.createdDate)
        assertEquals("2010-01-01 test", t3.text)
    }

    fun testGetCompletionDate() {
        var t = "x 2012-11-11 due:2014-10-10 rec:12w mail@example.com"
        assertEquals("2012-11-11", TTask(t).completionDate)
    }

    fun testGetCreatedDate() {
        var t = "x 2012-11-11 due:2014-10-10 rec:12w mail@example.com"
        assertNull(TTask(t).createdDate)
        t = "x 2012-11-11 2013-11-11 due:2014-10-10 rec:12w mail@example.com"
        assertEquals("2013-11-11", TTask(t).createdDate)
        t = "2013-11-11 due:2014-10-10 rec:12w mail@example.com"
        assertEquals("2013-11-11", TTask(t).createdDate)
    }

    fun testShow() {
        var t = TTask("x 2012-11-11 due:2014-10-10 morgen rec:12w mail@example.com")

        val text = t.tokens.filter {
            it.type == "TextToken"
        }.map{it.text}.joinToString(" ")
        assertEquals("morgen", text)
    }
}