package nl.mpcjanssen.simpletask.client

import nl.mpcjanssen.simpletask.task.Task

interface BackupInterface {
    fun backup(name: String, lines: List<String>)
}
