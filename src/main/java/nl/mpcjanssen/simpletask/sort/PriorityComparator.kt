package nl.mpcjanssen.simpletask.sort


import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class PriorityComparator : Comparator<Task> {

    override fun compare(a: Task?, b: Task?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val prioA = a.priority
        val prioB = b.priority

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
