package nl.mpcjanssen.simpletask.dao

import nl.mpcjanssen.simpletask.task.Task

/**
 * Created by Mark on 22-1-2016.
 */
abstract class CachedTask {
    abstract var text: String
    private var cachedTask: Task? = null
    val task: Task
    get() {
        val result = cachedTask ?: Task(text)
        cachedTask = result
        return result
    }
}