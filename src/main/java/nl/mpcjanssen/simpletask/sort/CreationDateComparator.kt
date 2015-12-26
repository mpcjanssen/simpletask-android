package nl.mpcjanssen.simpletask.sort

import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.isEmptyOrNull


import java.util.*

class CreationDateComparator : Comparator<Task> {

    override fun compare(a: Task?, b: Task?): Int {
        val result: Int
        if (isEmptyOrNull(a!!.createDate) && isEmptyOrNull(b!!.createDate)) {
            result = 0
        } else if (isEmptyOrNull(a.createDate)) {
            result = 1
        } else if (isEmptyOrNull(b!!.createDate)) {
            result = -1
        } else {
            val dateA = DateTime(a.createDate)
            val dateB = DateTime(b.createDate)
            result = dateA.compareTo(dateB)
        }
        return result
    }
}
