package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoListItem
import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<TodoListItem> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: TodoListItem?, t2: TodoListItem?): Int {
        val a = t1?.task ?: Task("")
        val b = t2?.task ?: Task("")
        return stringComp.compare(a.showParts(TToken.TEXT),b.showParts(TToken.TEXT))
    }
}
