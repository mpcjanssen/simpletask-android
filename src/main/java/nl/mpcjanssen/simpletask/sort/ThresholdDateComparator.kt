package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class ThresholdDateComparator : Comparator<Task> {

    override fun compare(a: Task?, b: Task?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val result: Int
        if (a.thresholdDate == null && b.thresholdDate == null) {
            result = 0
        } else if (a.thresholdDate == null) {
            result = 1
        } else if (b.thresholdDate == null) {
            result = -1
        } else {
            result = a.thresholdDate!!.compareTo(b.thresholdDate)
        }
        return result
    }
}
