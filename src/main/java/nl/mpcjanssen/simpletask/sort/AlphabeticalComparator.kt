package nl.mpcjanssen.simpletask.sort


import nl.mpcjanssen.simpletask.task.TEXT
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoItem

import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<TodoItem> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: TodoItem?, t2: TodoItem?): Int {
        var a = t1?.task ?: Task("")
        var b = t2?.task ?: Task("")
        return stringComp.compare(a.showParts(TEXT),b.showParts(TEXT))
    }
}
