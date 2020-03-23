package nl.mpcjanssen.simpletask.remote

import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import java.io.File
import java.io.IOException
import kotlin.reflect.KClass

/**
 * Interface definition of the storage backend used.
 * All functions are run in a background thread and are properly queued to prevent
 * concurrent Todo list modification.
 * The implementers of this interface should throw exceptions if anything goes wrong. The logic for handling the
 * errors should be implemented in the users of the FileStore
 * Because some FileStores might be accessing the network to do their job,
 * all file related actions should never be performed on the main thread.

 */
interface IFileStore {
    val isAuthenticated: Boolean

    @Throws(IOException::class)
    fun loadTasksFromFile(path: String): RemoteContents
    fun saveTasksToFile(path: String, lines: List<String>, lastRemote: String?, eol: String) : String

    // Handle login and logout
    fun loginActivity(): KClass<*>?
    fun logout()

    @Throws(IOException::class)
    fun appendTaskToFile(path: String, lines: List<String>, eol: String)

    fun readFile(file: String, fileRead: (String) -> Unit)
    fun writeFile(file: String, contents: String)

    val isOnline: Boolean

    // Retrieve the remote file version
    fun getRemoteVersion(filename: String): String?

    // Return the default todo.txt path
    fun getDefaultPath(): String

    // Return files and subfolders in path. If txtOnly is true only *.txt files will be returned.
    fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry>

    // Allow the FileStore to signal that the remote
    // todoFile changed. Call this in the filestore code
    // to force file sync
    @Suppress
    fun remoteTodoFileChanged() {
        broadcastFileSync(TodoApplication.app.localBroadCastManager)

    }

    fun doneFile(todoFileName: String): String {
        return sibling(todoFileName, "done.txt")
    }

    fun parent(todoFileName: String): String {
        return File(todoFileName).parent
    }
    fun sibling(todoFileName: String, name: String): String {
        return File(parent(todoFileName), name).canonicalPath
    }

    // Generic special folder names for use in File dialogs
    companion object {
        val ROOT_DIR = "/"
        val PARENT_DIR = ".."

    }
}

// Data class to return the lines and verion of a remote file.
data class RemoteContents(val remoteId: String, val contents: List<String>)

// Generic file entry class for use in File dialogs
data class FileEntry(val name: String, val isFolder: Boolean)



