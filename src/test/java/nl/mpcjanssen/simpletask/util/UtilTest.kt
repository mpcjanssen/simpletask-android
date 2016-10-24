package nl.mpcjanssen.simpletask.util

import junit.framework.TestCase
import nl.mpcjanssen.simpletask.dao.gentodo.TodoItem

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class UtilTest : TestCase() {
    fun testAddHeaderLines() {
        val tasks = ArrayList<Task>()
        tasks.add(Task("@Home h:1"))
        tasks.add(Task("@Work h:1"))
        tasks.add(Task("@College h:1"))
        val items = ArrayList<TodoItem>()
        for (t in tasks) {
            items.add(TodoItem(0, t, false))
        }
        assertEquals(6, addHeaderLines(items, ArrayList(listOf<String>("by_context")) , "none", false, null).size)

    }

    fun testIntersectUnion() {
        // Don't crash on an empty lists
        assertEquals(HashSet<String>(), ArrayList<HashSet<String>>().intersection())
        assertEquals(HashSet<String>(), ArrayList<HashSet<String>>().union())
    }
}
