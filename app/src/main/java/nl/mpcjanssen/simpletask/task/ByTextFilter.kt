package nl.mpcjanssen.simpletask.task

import nl.mpcjanssen.simpletask.Interpreter
import java.util.*

/**
 * A applyFilter that matches Tasks containing the specified text
 */
class ByTextFilter(private val moduleName : String, searchText: String?, private val isCaseSensitive: Boolean) : TaskFilter {
    val text = searchText ?: ""

    private val parts: List<String>
        get() = cased(text).split("\\s".toRegex()).dropLastWhile { it.isEmpty() }

    override fun apply(task: Task): Boolean {
        return scriptResult(task) ?:
            cased(task.text).let { taskText ->
                !parts.any {
                    it.isNotEmpty() && !taskText.contains(it)
                }
            }
    }

    private fun scriptResult(task: Task): Boolean? {
        return Interpreter.onTextSearchCallback(moduleName, task.text, text, isCaseSensitive)
    }


    private fun cased(t: String): String {
        return if (isCaseSensitive) t else t.toUpperCase(Locale.getDefault())
    }
}
