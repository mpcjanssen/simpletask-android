package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.Priority
import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import java.util.*

class MultiComparator(sorts: ArrayList<String>, today: String, caseSensitve: Boolean, createAsBackup: Boolean, moduleName: String? = null) {
    var comparator : Comparator<Task>? = null

    var fileOrder = true

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
            var comp: (Task) -> Comparable<*>
            val last_date = "9999-99-99"
            when (sortType) {
                "file_order" -> {
                    fileOrder = !reverse
                    break@label
                }
                "by_context" -> comp = { it.lists.sorted().firstOrNull() ?: "" }
                "by_project" -> comp = { it.tags.sorted().firstOrNull() ?: "" }
                "alphabetical" -> comp = if (caseSensitve) {
                    { it -> it.showParts(TToken.TEXT) }
                } else {
                    { it -> it.showParts(TToken.TEXT).toLowerCase(Locale.getDefault()) }
                }
                "by_prio" -> comp = {
                    val prio = it.priority
                    if (prio == Priority.NONE) {
                        "ZZZZ"
                    } else {
                        prio.code
                    }
                }
                "completed" -> comp = { it.isCompleted() }
                "by_creation_date" -> comp = { it.createDate ?: last_date }
                "in_future" -> comp = { it.inFuture(today) }
                "by_due_date" -> comp = { it.dueDate ?: last_date }
                "by_threshold_date" -> comp = {
                    val fallback = if (createAsBackup) it.createDate ?: last_date else last_date
                    it.thresholdDate ?: fallback
                }
                "by_completion_date" -> comp = { it.completionDate ?: last_date }
                "by_lua" -> {
                    if (moduleName == null || !LuaInterpreter.hasOnSortCallback(moduleName)) {
                       continue@label
                    }
                    comp = {
                        val str = LuaInterpreter.onSortCallback(moduleName, it)
                        str
                    }
                }
                else -> {
                    log.warn("MultiComparator", "Unknown sort: " + sort)
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
