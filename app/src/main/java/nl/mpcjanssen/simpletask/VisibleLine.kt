package nl.mpcjanssen.simpletask

import nl.mpcjanssen.simpletask.task.Task

interface VisibleLine {
    val header: Boolean
    val task : Task?
    val title: String?
}

data class TaskLine(override val task: Task) : VisibleLine {
    override val title: String?
        get() = null
    override val header = false

}

data class HeaderLine(override var title: String) : VisibleLine {
    override val task: Task?
        get() =  null
    override val header = true
}