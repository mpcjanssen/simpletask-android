package nl.mpcjanssen.simpletask.dao

import nl.mpcjanssen.simpletask.task.Task

/**
 * Created by Mark on 22-1-2016.
 */
abstract class CachedTask {
    abstract var text: String
    val task : Task by lazy {Task(text)}
}