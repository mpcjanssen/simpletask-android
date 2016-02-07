package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.TodoListItem

/**
 * Created by Mark on 2015-08-05.
 */
interface VisibleLine {
    val header: Boolean
    val task : TodoListItem?
    val title: String?
}

data class TaskLine(override val task: TodoListItem) : VisibleLine {
    override val title  = null
    override val header = false

}

data class HeaderLine(override var title: String) : VisibleLine {
    override val task  = null
    override val header = true
}