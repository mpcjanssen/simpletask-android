package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import java.io.File
import java.io.IOException

/**
 * Interface definition of the storage backend used.
 * All functions are run in a background thread and are properly queued to prevent
 * concurrent Todo list modification.
 * The implementers of this interface should throw exceptions if anything goes wrong. The logic for handling the
 * errors should be implemented in the users of the FileStore

 */
interface FileStoreInterface {
    val isAuthenticated: Boolean

    @Throws(IOException::class)
    fun loadTasksFromFile(path: String, eol: String): RemoteContents
    fun saveTasksToFile(path: String, lines: List<String>, eol: String) : String

    fun startLogin(caller: Activity)
    fun logout()

    @Throws(IOException::class)
    fun appendTaskToFile(path: String, lines: List<String>, eol: String)

    @Throws(IOException::class)
    fun readFile(file: String, fileRead: FileReadListener?): String
    fun writeFile(file: File, contents: String)

    fun supportsSync(): Boolean

    fun getWritePermission(act : Activity, activityResult: Int): Boolean


    val isOnline: Boolean

    interface FileSelectedListener {
        fun fileSelected(file: String)
    }

    interface FileChangeListener {
        fun fileChanged(newName: String?)
    }

    interface FileReadListener {
        fun fileRead(contents: String?)
    }

    // Retrieve the remote file version
    fun getRemoteVersion(filename: String): String?

    fun getDefaultPath(): String

    fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry>

    // Allow the FileStore to signal that the remote
    // todoFile changed. Call this in the filestore code
    // to force file sync
    fun remoteTodoFileChanged() {
        broadcastFileSync(TodoApplication.app.localBroadCastManager)

    }

    companion object {
        val ROOT_DIR = "/"
        val PARENT_DIR = ".."

    }
}

data class RemoteContents(val remoteId: String, val contents: List<String>)
data class FileEntry(val name: String, val isFolder: Boolean)



