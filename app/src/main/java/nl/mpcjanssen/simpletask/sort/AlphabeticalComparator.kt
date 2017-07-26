package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<Task> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: Task?, t2: Task?): Int {
        val a = t1 ?: Task("")
        val b = t2 ?: Task("")
        return stringComp.compare(a.showParts(TToken.TEXT), b.showParts(TToken.TEXT))
    }
}
