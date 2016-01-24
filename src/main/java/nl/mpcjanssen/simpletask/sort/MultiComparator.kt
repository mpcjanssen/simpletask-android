package nl.mpcjanssen.simpletask.sort


import nl.mpcjanssen.simpletask.ActiveFilter
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.dao.gen.TodoListItem

import java.util.*

class MultiComparator(sorts: ArrayList<String>, caseSensitve: Boolean, createAsBackup: Boolean) : Comparator<TodoListItem> {
    var comparators : Comparator<TodoListItem>? = null
    val defaultComparator = FileOrderComparator()
    val TAG = "MultiComparator"
    init {
        val log = Logger

        label@ for (sort in sorts) {
            val parts = sort.split(ActiveFilter.SORT_SEPARATOR.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            var reverse = false
            val sortType: String
            if (parts.size == 1) {
                // support older shortcuts and widgets
                reverse = false
                sortType = parts[0]
            } else {
                sortType = parts[1]
                if (parts[0] == ActiveFilter.REVERSED_SORT) {
                    reverse = true
                }
            }
            var comp : Comparator<TodoListItem>
            when (sortType) {
                "file_order" -> comp = FileOrderComparator()
                "by_context" -> comp = ContextComparator(caseSensitve)
                "by_project" -> comp = ProjectComparator(caseSensitve)
                "alphabetical" -> comp = AlphabeticalComparator(caseSensitve)
                "by_prio" -> comp = PriorityComparator()
                "completed" -> comp = CompletedComparator()
                "by_creation_date" -> comp = CreationDateComparator()
                "in_future" -> comp = FutureComparator()
                "by_due_date" -> comp = DueDateComparator()
                "by_threshold_date" -> comp = ThresholdDateComparator(createAsBackup)
                else -> {
                    log.warn(TAG, "Unknown sort: " + sort)
                    continue@label
                }
            }
            if (reverse) {
                comp = comp.reversed()
            }

            comparators = comparators?.then(comp) ?:  comp
        }
    }

    override fun compare(o1: TodoListItem, o2: TodoListItem): Int {
        return comparators?.compare(o1, o2)?:defaultComparator.compare(o1,o2)
    }
}
