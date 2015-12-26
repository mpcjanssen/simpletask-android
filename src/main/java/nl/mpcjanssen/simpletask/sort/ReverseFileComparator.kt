package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class ReverseFileComparator  (val taskList : List<Task>) :  Comparator<Task> {
    override fun compare(a: Task?, b: Task?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val indexA = taskList.indexOf(a)
        val indexB = taskList.indexOf(b)
        return indexA.compareTo(indexB)
    }
}