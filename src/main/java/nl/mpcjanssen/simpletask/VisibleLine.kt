package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.TodoItem

interface VisibleLine {
    val header: Boolean
    val item : TodoItem?
    val title: String?
}

data class TaskLine(override val item: TodoItem) : VisibleLine {
    override val title  = null
    override val header = false

}

data class HeaderLine(override var title: String) : VisibleLine {
    override val item  = null
    override val header = true
}