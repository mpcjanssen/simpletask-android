package nl.mpcjanssen.simpletask.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import android.util.Log
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.writeToFile
import java.io.File
import java.io.FilenameFilter
import java.util.*
import kotlin.reflect.KClass

object FileStore : IFileStore {
    override fun getRemoteVersion(filename: String): String {
        return File(filename).lastModified().toString()
    }

    override val isOnline = true
    private val TAG = "FileStore"
    private var observer: TodoObserver? = null


    init {
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Default path: ${getDefaultPath()}")
        observer = null

    }

    override val isAuthenticated: Boolean
        get() {
            val permissionCheck = ContextCompat.checkSelfPermission(TodoApplication.app,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return permissionCheck == PackageManager.PERMISSION_GRANTED
        }

    override fun loadTasksFromFile(path: String): RemoteContents {
        Log.i(TAG, "Loading tasks")
        val file = File(path)
        val lines = file.readLines()
        Log.i(TAG, "Read ${lines.size} lines from $path")
        setWatching(path)
        return RemoteContents(file.lastModified().toString(), lines)
    }

    override fun writeFile(path: String, contents: String) {
        Log.i(TAG, "Writing file to  $path")
        File(path).writeText(contents)
    }

    override fun readFile(file: String, fileRead: (String) -> Unit) {
        Log.i(TAG, "Reading file: {}" + file)
        val contents: String
        val lines = File(file).readLines()
        contents = join(lines, "\n")
        fileRead(contents)
    }

    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    private fun setWatching(path: String) {
        Log.i(TAG, "Observer: adding folder watcher on ${File(path).parentFile.absolutePath}")
        val obs = observer
        if (obs != null && path == obs.path) {
            Log.w(TAG, "Observer: already watching: $path")
            return
        } else if (obs != null) {
            Log.w(TAG, "Observer: already watching different path: ${obs.path}")
            obs.ignoreEvents(true)
            obs.stopWatching()
            observer = null
        }
        observer = TodoObserver(path)
        Log.i(TAG, "Observer: modifying done")
    }

    override fun saveTasksToFile(path: String, lines: List<String>, lastRemote: String?, eol: String) : String {
        Log.i(TAG, "Saving tasks to file: $path")
        val obs = observer
        obs?.ignoreEvents(true)
        val file = File(path)
        writeToFile(lines, eol, file, false)
        obs?.delayedStartListen(1000)
        return file.lastModified().toString()
    }

    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        Log.i(TAG, "Appending ${lines.size} tasks to $path")
        writeToFile(lines, eol, File(path), true)
    }

    override fun logout() {

    }

    override fun getDefaultPath(): String {
        return "${Environment.getExternalStorageDirectory()}/data/nl.mpcjanssen.simpletask/todo.txt"
    }

    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {
        val result = ArrayList<FileEntry>()
        val file = File(path)
        if (path == "/") {
            result.add(FileEntry(Environment.getExternalStorageDirectory().path, true))
        }

        val filter = FilenameFilter { dir, filename ->
            val sel = File(dir, filename)
            if (!sel.canRead())
                false
            else {
                if (sel.isDirectory) {
                    result.add(FileEntry(sel.name, true))
                } else {
                    !txtOnly || filename.toLowerCase(Locale.getDefault()).endsWith(".txt")
                    result.add(FileEntry(sel.name, false))
                }
            }
        }
        // Run the file applyFilter for side effects
        file.list(filter)
        return result
    }

    class TodoObserver(val path: String) : FileObserver(File(path).parentFile.absolutePath) {
        private val TAG = "FileWatchService"
        private val fileName: String
        private var ignoreEvents: Boolean = false
        private val handler: Handler

        private val delayedEnable = Runnable {
            Log.i(TAG, "Observer: Delayed enabling events for: " + path)
            ignoreEvents(false)
        }

        init {
            this.startWatching()
            this.fileName = File(path).name
            Log.i(TAG, "Observer: creating observer on: {}")
            this.ignoreEvents = false
            this.handler = Handler(Looper.getMainLooper())

        }

        fun ignoreEvents(ignore: Boolean) {
            Log.i(TAG, "Observer: observing events on " + this.path + "? ignoreEvents: " + ignore)
            this.ignoreEvents = ignore
        }

        override fun onEvent(event: Int, eventPath: String?) {
            if (eventPath != null && eventPath == fileName) {
                Log.d(TAG, "Observer event: $path:$event")
                if (event == FileObserver.CLOSE_WRITE ||
                        event == FileObserver.MODIFY ||
                        event == FileObserver.MOVED_TO) {
                    if (ignoreEvents) {
                        Log.i(TAG, "Observer: ignored event on: " + path)
                    } else {
                        Log.i(TAG, "File changed {}" + path)
                        FileStore.remoteTodoFileChanged()
                    }
                }
            }

        }

        fun delayedStartListen(ms: Int) {
            // Cancel any running timers
            handler.removeCallbacks(delayedEnable)
            // Reschedule
            Log.i(TAG, "Observer: Adding delayed enabling to todoQueue")
            handler.postDelayed(delayedEnable, ms.toLong())
        }

    }
}
