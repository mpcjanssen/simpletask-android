package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class ContextComparator(caseSensitive: Boolean) : Comparator<Task> {

    private val mStringComparator: AlphabeticalStringComparator

    init {
        this.mStringComparator = AlphabeticalStringComparator(caseSensitive)
    }

    override fun compare(a: Task?, b: Task?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val contextsA = a.lists.toMutableList()
        val contextsB = b.lists.toMutableList()

        if (contextsA.isEmpty() && contextsB.isEmpty()) {
            return 0
        } else if (contextsA.isEmpty()) {
            return -1
        } else if (contextsB.isEmpty()) {
            return 1
        } else {
            contextsA.sort()
            contextsB.sort()
            return mStringComparator.compare(contextsA[0], contextsB[0])
        }
    }
}
