package nl.mpcjanssen.simpletask.dao

import nl.mpcjanssen.simpletask.task.Task

/**
 * Superclass for the TodoListItems. Will parse the task on demand.
 */
abstract class CachedTask {
    abstract var text: String
    val task : Task by lazy {Task(text)}
}