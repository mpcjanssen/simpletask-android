package nl.mpcjanssen.simpletask.sort

import junit.framework.TestCase
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.asTask

import java.util.ArrayList
import java.util.Collections

class SortTest : TestCase() {
    fun testAlphabeticalSort1() {
        val tasks = ArrayList<Task>()
        val t1 = Task("2011-01-01 B")
        val t2 = Task("2012-01-01 A")
        tasks.add(t1)
        tasks.add(t2)
        val items = tasks.map {it.asTask()}
        assertEquals(t1, items[0].task)
        Collections.sort(items , AlphabeticalComparator(true))
        assertEquals(t2, items[0].task)
    }

    fun testAlphabeticalSort2() {
        val tasks = ArrayList<Task>()
        val t1 = Task("(A) B")
        val t2 = Task("(B) A")
        tasks.add(t1)
        tasks.add(t2)
        val items = tasks.map {it.asTask() }
        assertEquals(t1, items[0].task)
        Collections.sort(items, AlphabeticalComparator(true))
        assertEquals(t2, items[0].task)
    }
}
