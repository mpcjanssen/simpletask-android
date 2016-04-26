package nl.mpcjanssen.simpletask.util

import junit.framework.TestCase
import nl.mpcjanssen.simpletask.dao.todo.TodoItem

import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.asTodoItem

import java.util.*

/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

class UtilTest : TestCase() {
    fun testAddHeaderLines() {
        val tasks = ArrayList<Task>()
        tasks.add(Task("@Home h:1"))
        tasks.add(Task("@Work h:1"))
        tasks.add(Task("@College h:1"))
        val items = ArrayList<TodoItem>()
        for (t in tasks) {
            items.add(t.asTodoItem())
        }
        assertEquals(6, addHeaderLines(items, "by_context", "none").size)

    }

    fun testIntersectUnion() {
        // Don't crash on an empty lists
        assertEquals(HashSet<String>(), ArrayList<HashSet<String>>().intersection())
        assertEquals(HashSet<String>(), ArrayList<HashSet<String>>().union())
    }
}
