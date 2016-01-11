package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.token.Token
import java.util.*

class AlphabeticalComparator(caseSensitive: Boolean) : Comparator<Task> {
    val stringComp = AlphabeticalStringComparator(caseSensitive)
    override fun compare(a: Task?, b: Task?): Int {
        var a = a
        var b = b
        if (a == null) {
            a = Task("")
        }
        if (b == null) {
            b = Task("")
        }
        return stringComp.compare(a.showParts(TToken.TEXT),b.showParts(TToken.TEXT))
    }
}
