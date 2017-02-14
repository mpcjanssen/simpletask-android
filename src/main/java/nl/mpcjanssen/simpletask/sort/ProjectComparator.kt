package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class ProjectComparator(caseSensitive: Boolean) : Comparator<Task> {

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
        val projectsA = a.tags.toMutableList()
        val projectsB = b.tags.toMutableList()

        if (projectsA.isEmpty() && projectsB.isEmpty()) {
            return 0
        } else if (projectsA.isEmpty()) {
            return 1
        } else if (projectsB.isEmpty()) {
            return -1
        } else {
            Collections.sort(projectsA)
            Collections.sort(projectsB)
            return mStringComparator.compare(projectsA[0], projectsB[0])
        }
    }
}
