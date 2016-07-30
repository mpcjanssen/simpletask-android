package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.dao.gen.TodoItem

interface VisibleLine {
    val header: Boolean
    val task : TodoItem?
    val title: String?
}

data class TaskLine(override val task: TodoItem) : VisibleLine {
    override val title  = null
    override val header = false

}

data class HeaderLine(override var title: String) : VisibleLine {
    override val task  = null
    override val header = true
}