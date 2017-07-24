package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class CompReverser(val comp: Comparator<Task>) : Comparator<Task> {
    override fun compare(t1: Task?, t2: Task?): Int {
        return -comp.compare(t1, t2)
    }

}