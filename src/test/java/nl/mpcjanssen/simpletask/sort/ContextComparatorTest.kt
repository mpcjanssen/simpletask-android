package nl.mpcjanssen.simpletask.sort

import junit.framework.TestCase
import nl.mpcjanssen.simpletask.dao.gen.TodoListItem
import nl.mpcjanssen.simpletask.task.Task

import java.util.ArrayList
import java.util.Collections

class ContextComparatorTest : TestCase() {

    @Throws(Exception::class)
    fun testCompare() {
        val tasks = ArrayList<Task>()
        tasks.add(Task("Test @b"))
        tasks.add(Task("Test @a"))
        tasks.add(Task("Test"))
        tasks.add(Task("Loop @a"))
        Collections.sort(tasks.map {it -> TodoListItem(0,it,false)}, ContextComparator(true))
        assertEquals("Test", tasks[0].inFileFormat())
        assertEquals("Test @a", tasks[1].inFileFormat())
        assertEquals("Loop @a", tasks[2].inFileFormat())
        assertEquals("Test @b", tasks[3].inFileFormat())
    }
}