package nl.mpcjanssen.simpletask.sort


import nl.mpcjanssen.simpletask.task.TodoItem
import java.util.*

class CompletedComparator : Comparator<TodoItem> {

    override fun compare(a: TodoItem?, b: TodoItem?): Int {
        if (a == null && b == null) return 0
        if (a == null) return 1
        if (b == null) return -1

        val completeA = if (a.task.isCompleted()) 1 else 0
        val completeB = if (b.task.isCompleted()) 1 else 0
        return (completeA - completeB)
    }
}
