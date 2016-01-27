package nl.mpcjanssen.simpletask.task

interface TaskFilter {
    fun apply(t: Task): Boolean
}
