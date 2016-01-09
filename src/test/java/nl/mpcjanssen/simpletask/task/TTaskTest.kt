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
    }

    fun testLexing() {
        assertEquals(listOf("ab", " ", "b", " ", " ", "d", " ", "s"), "ab b  d s".lex())
    }
}