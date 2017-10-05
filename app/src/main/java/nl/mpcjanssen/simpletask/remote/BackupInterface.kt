package nl.mpcjanssen.simpletask.remote

import nl.mpcjanssen.simpletask.task.Task

interface BackupInterface {
    fun backup(name: String, lines: List<Task>)
}
