package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.dao.gen.TodoItem
import java.util.*


class FileOrderComparator  : Comparator<TodoItem> {
    override fun compare(a: TodoItem?, b: TodoItem?): Int {
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