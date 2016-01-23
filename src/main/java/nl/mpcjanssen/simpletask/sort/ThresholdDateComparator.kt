package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.dao.gen.TodoListItem
import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class ThresholdDateComparator(val createAsBackup: Boolean) : Comparator<TodoListItem> {

    override fun compare(a: TodoListItem?, b: TodoListItem?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val result: Int

        var dateA = a.task.thresholdDate
        var dateB = b.task.thresholdDate


        // Use create date as threshold date
        // if configured in the settings.
        if (createAsBackup) {
            dateA = dateA ?: a.task.createDate
            dateB = dateB ?: b.task.createDate
        }
        if (dateA == null && dateB == null) {
            result = 0
        } else if (dateA == null) {
            result = 1
        } else if (dateB == null) {
            result = -1
        } else {
            result = dateA.compareTo(dateB)
        }
        return result
    }
}
