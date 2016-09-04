package nl.mpcjanssen.simpletask.task

interface TaskFilter {
    fun apply(task: Task): Boolean
}
