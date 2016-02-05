package nl.mpcjanssen.simpletask.sort


import nl.mpcjanssen.simpletask.task.TodoListItem
import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class FutureComparator(val today: String) : Comparator<TodoListItem> {

    override fun compare(a: TodoListItem?, b: TodoListItem?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val futureA = if (a.task.inFuture(today)) 1 else 0
        val futureB = if (b.task.inFuture(today)) 1 else 0
        return (futureA - futureB)
    }
}
