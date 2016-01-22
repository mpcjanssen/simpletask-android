package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<Task> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: Task?, t2: Task?): Int {
        var a = t1
        var b = t2
        if (a == null) {
            a = Task("")
        }
        if (b == null) {
            b = Task("")
        }
        return stringComp.compare(a.showParts(TToken.TEXT),b.showParts(TToken.TEXT))
    }
}
