package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class DueDateComparator : Comparator<Task> {

    override fun compare(a: Task?, b: Task?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val result: Int
        if (a.dueDate == null && b.dueDate == null) {
            result = 0
        } else if (a.dueDate == null) {
            result = 1
        } else if (b.dueDate == null) {
            result = -1
        } else {
            result = a.dueDate!!.compareTo(b.dueDate)
        }
        return result
    }
}
