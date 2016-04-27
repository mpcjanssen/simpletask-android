package nl.mpcjanssen.simpletask.sort

import junit.framework.TestCase

import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.asTodoItem

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
        val items = tasks.map {it.asTodoItem() }
        Collections.sort(items , ContextComparator(true))
        assertEquals("Test", items[0].task.inFileFormat())
        assertEquals("Test @a", items[1].task.inFileFormat())
        assertEquals("Loop @a", items[2].task.inFileFormat())
        assertEquals("Test @b", items[3].task.inFileFormat())
    }
}