package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.dao.gentodo.TodoItem
import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<TodoItem> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: TodoItem?, t2: TodoItem?): Int {
        val a = t1?.task ?: Task("")
        val b = t2?.task ?: Task("")
        return stringComp.compare(a.showParts(TToken.TEXT),b.showParts(TToken.TEXT))
    }
}
