package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TodoListItem
import java.util.*

class PriorityComparator : Comparator<TodoListItem> {

    override fun compare(a: TodoListItem?, b: TodoListItem?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val prioA = a.task.priority
        val prioB = b.task.priority

        if (prioA.code == prioB.code) {
            return 0
        } else if (prioA.inFileFormat() == "") {
            return 1
        } else if (prioB.inFileFormat() == "") {
            return -1
        } else {
            return prioA.compareTo(prioB)
        }
    }
}
