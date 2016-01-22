package nl.mpcjanssen.simpletask.sort


import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class CompletedComparator : Comparator<Task> {

    override fun compare(a: Task?, b: Task?): Int {
        if (a == null && b == null) return 0
        if (a == null) return 1
        if (b == null) return -1

        val completeA = if (a.isCompleted()) 1 else 0
        val completeB = if (b.isCompleted()) 1 else 0
        return (completeA - completeB)
    }
}
