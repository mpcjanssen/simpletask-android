package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.task.TodoItem
import java.util.*

class ProjectComparator(caseSensitive: Boolean) : Comparator<TodoItem> {

    private val mStringComparator: AlphabeticalStringComparator

    init {
        this.mStringComparator = AlphabeticalStringComparator(caseSensitive)
    }


    override fun compare(a: TodoItem?, b: TodoItem?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val projectsA = a.task.tags.toMutableList()
        val projectsB = b.task.tags.toMutableList()

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
