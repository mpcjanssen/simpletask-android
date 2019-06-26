package nl.mpcjanssen.simpletask.util

import junit.framework.TestCase

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class UtilTest : TestCase() {
    fun testAddHeaderLines() {
        val tasks = ArrayList<Task>()
        tasks.add(Task("@Home h:1"))
        tasks.add(Task("@Work h:1"))
        tasks.add(Task("@College h:1"))
        val headers = addHeaderLines(tasks, ArrayList(listOf("by_context")) , "none", false, null)
        assertEquals(6, headers.size)
    }

    fun testAddHeaderLinesSort() {
        val tasks = ArrayList<Task>()
        tasks.add(Task("@a @b"))
        tasks.add(Task("@b @a"))
        val headers = addHeaderLines(tasks, ArrayList(listOf("by_context")) , "none", false, null)
        assertEquals(3, headers.size)
    }
}
