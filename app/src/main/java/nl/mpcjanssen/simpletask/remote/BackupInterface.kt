package nl.mpcjanssen.simpletask.remote

/**
 * Interface definition of the storage backend used.

 * Uses events to communicate with the application. Currently supported are SYNC_START, SYNC_DONE and FILE_CHANGED.
 */
interface BackupInterface {
    fun backup(name: String, contents: String)
}
