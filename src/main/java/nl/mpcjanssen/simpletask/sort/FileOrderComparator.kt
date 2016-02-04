package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TodoListItem
import nl.mpcjanssen.simpletask.task.Task
import java.util.*


class FileOrderComparator  : Comparator<TodoListItem> {
    override fun compare(a: TodoListItem?, b: TodoListItem?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        return (a.line - b.line).toInt()
    }
}