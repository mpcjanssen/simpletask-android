package nl.mpcjanssen.simpletask.sort

import nl.mpcjanssen.simpletask.dao.gen.TodoListItem
import java.util.*

class ProjectComparator(caseSensitive: Boolean) : Comparator<TodoListItem> {

    private val mStringComparator: AlphabeticalStringComparator

    init {
        this.mStringComparator = AlphabeticalStringComparator(caseSensitive)
    }


    override fun compare(a: TodoListItem?, b: TodoListItem?): Int {
        if (a === b) {
            return 0
        } else if (a == null) {
            return -1
        } else if (b == null) {
            return 1
        }
        val projectsA = a.task.tags.toArrayList()
        val projectsB = b.task.tags.toArrayList()

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
