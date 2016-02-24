package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TodoListItem
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoList
import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<TodoListItem> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: TodoListItem?, t2: TodoListItem?): Int {
        var a = t1?.task?.text ?: ""
        var b = t2?.task?.text ?: ""


        return stringComp.compare(a.filter { it.isLetter() },b.filter {it.isLetter()})
    }
}
