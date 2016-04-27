package nl.mpcjanssen.simpletask.sort


import nl.mpcjanssen.simpletask.task.TEXT
import nl.mpcjanssen.simpletask.task.Task

import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<Task> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(t1: Task?, t2: Task?): Int {
        var a = t1 ?: Task("")
        var b = t2 ?: Task("")
        return stringComp.compare(a.showParts(TEXT),b.showParts(TEXT))
    }
}
