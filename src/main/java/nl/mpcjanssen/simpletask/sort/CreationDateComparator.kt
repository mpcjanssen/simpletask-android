package nl.mpcjanssen.simpletask.sort

import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.dao.gen.TodoListItem
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.isEmptyOrNull


import java.util.*

class CreationDateComparator : Comparator<TodoListItem> {

    override fun compare(a: TodoListItem?, b: TodoListItem?): Int {
        val result: Int
        if (isEmptyOrNull(a!!.task.createDate) && isEmptyOrNull(b!!.task.createDate)) {
            result = 0
        } else if (isEmptyOrNull(a.task.createDate)) {
            result = 1
        } else if (isEmptyOrNull(b!!.task.createDate)) {
            result = -1
        } else {
            val dateA = DateTime(a.task.createDate)
            val dateB = DateTime(b.task.createDate)
            result = dateA.compareTo(dateB)
        }
        return result
    }
}
