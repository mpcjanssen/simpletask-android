package nl.mpcjanssen.simpletask.sort



import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class FutureComparator(val today: String) : Comparator<Task> {

    override fun compare(a: Task?, b: Task?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val futureA = if (a.inFuture(today)) 1 else 0
        val futureB = if (b.inFuture(today)) 1 else 0
        return (futureA - futureB)
    }
}
