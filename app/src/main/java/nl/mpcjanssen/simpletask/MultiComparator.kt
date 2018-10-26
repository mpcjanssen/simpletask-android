package nl.mpcjanssen.simpletask

import android.util.Log
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TextToken
import nl.mpcjanssen.simpletask.util.alfaSort
import java.util.*

class MultiComparator(sorts: ArrayList<String>, today: String, caseSensitve: Boolean, createAsBackup: Boolean, moduleName: String? = null) {
    var comparator : Comparator<Task>? = null

    var fileOrder = true

    init {
        label@ for (sort in sorts) {
            val parts = sort.split(Query.SORT_SEPARATOR.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            var reverse = false
            val sortType: String
            if (parts.size == 1) {
                // support older shortcuts and widgets
                reverse = false
                sortType = parts[0]
            } else {
                sortType = parts[1]
                if (parts[0] == Query.REVERSED_SORT) {
                    reverse = true
                }
            }
            var comp: (Task) -> Comparable<*>
            val lastDate = "9999-99-99"
            when (sortType) {
                "file_order" -> {
                    fileOrder = !reverse
                    break@label
                }
                "by_context" -> comp = { it.lists?.let { alfaSort(it).firstOrNull() } ?: "" }
                "by_project" -> comp = { it.lists?.let { alfaSort(it).firstOrNull() } ?: "" }
                "alphabetical" -> comp = if (caseSensitve) {
                    { it.showParts { it.isAlpha() } }
                } else {
                    { it.showParts { it.isAlpha() }.toLowerCase(Locale.getDefault()) }
                }
                "by_prio" -> comp = { it.priority }
                "completed" -> comp = { it.isCompleted() }
                "by_creation_date" -> comp = { it.createDate ?: lastDate }
                "in_future" -> comp = { it.inFuture(today) }
                "by_due_date" -> comp = { it.dueDate ?: lastDate }
                "by_threshold_date" -> comp = {
                    val fallback = if (createAsBackup) it.createDate ?: lastDate else lastDate
                    it.thresholdDate ?: fallback
                }
                "by_completion_date" -> comp = { it.completionDate ?: lastDate }
                "by_lua" -> {
                    if (moduleName == null || !Interpreter.hasOnSortCallback(moduleName)) {
                       continue@label
                    }
                    comp = {
                        val str = Interpreter.onSortCallback(moduleName, it)
                        str
                    }
                }
                else -> {
                    Log.w("MultiComparator", "Unknown sort: $sort")
                    continue@label
                }
            }
            if (reverse) {
                comparator = comparator?.thenByDescending(comp) ?: compareByDescending(comp)
            } else {
                comparator = comparator?.thenBy(comp) ?: compareBy(comp)
            }
        }
    }

}
