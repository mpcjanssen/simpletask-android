package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import java.io.File
import java.io.IOException

/**
 * Interface definition of the storage backend used.

 */
interface FileStoreInterface {
    val isAuthenticated: Boolean

    data class RemoteContents(val remoteId : String, val contents: List<String> )

    @Throws(IOException::class)
    fun loadTasksFromFile(path: String, eol: String): RemoteContents
    fun saveTasksToFile(path: String, lines: List<String>, eol: String) : String

    fun startLogin(caller: Activity)
    fun logout()
    fun browseForNewFile(act: Activity, path: String, listener: FileSelectedListener, txtOnly: Boolean)

    @Throws(IOException::class)
    fun appendTaskToFile(path: String, lines: List<String>, eol: String)
    fun sync()

    @Throws(IOException::class)
    fun readFile(file: String, fileRead: FileReadListener?): String
    fun writeFile(file: File, contents: String)

    fun supportsSync(): Boolean
    fun changesPending(): Boolean

    fun getWritePermission(act : Activity, activityResult: Int): Boolean

    val isLoading: Boolean

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

    // Called when the main screen goes to the background and when it comes back
    fun pause(pause: Boolean) {
        // Do nothing by default
    }

    fun needsRefresh(currentVersion : String?): String?

    fun getVersion(filename: String): String?


}
