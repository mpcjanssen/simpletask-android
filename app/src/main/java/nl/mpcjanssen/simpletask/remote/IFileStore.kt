package nl.mpcjanssen.simpletask.remote

import android.net.Uri
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

    fun loadTasksFromFile(uri: Uri): RemoteContents
    fun saveTasksToFile(uri: Uri, lines: List<String>, eol: String) : String?
    fun appendTaskToFile(uri: Uri, lines: List<String>, eol: String)

    fun readFile(uri: Uri, fileRead: (contents: String) -> Unit)
    fun writeFile(uri: Uri, contents: String)

    // Retrieve the remote file version
    fun getRemoteVersion(uri: Uri?): String?
}

// Data class to return the lines and verion of a remote file.
data class RemoteContents(val remoteId: String?, val contents: List<String>)



