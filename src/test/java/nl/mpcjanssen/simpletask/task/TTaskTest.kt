package nl.mpcjanssen.simpletask.task

import junit.framework.TestCase

class TTaskTest : TestCase() {
    fun testParseIdempotence() {
        val t1 = "x 1231 2 123 312 +test"
        assertEquals(t1, TTask(t1).text)
    }
}