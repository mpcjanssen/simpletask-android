package nl.mpcjanssen.simpletask.task

import junit.framework.TestCase
import java.util.*

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
        assertEquals("2014-10-10".toDateTime(), TTask(t).dueDate)
        t = "x 2012-14-11 due:2014-99-99 rec:12w mail@example.com"
        assertNull(TTask(t).dueDate)
        t = "x 2012-14-11 rec:12w mail@example.com"
        assertEquals(null, TTask(t).dueDate)
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
}