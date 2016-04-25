package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TEXT
import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoListItem
import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<TodoListItem> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: TodoListItem?, t2: TodoListItem?): Int {
        var a = t1?.task ?: Task("")
        var b = t2?.task ?: Task("")
        return stringComp.compare(a.showParts(TEXT),b.showParts(TEXT))
    }
}
