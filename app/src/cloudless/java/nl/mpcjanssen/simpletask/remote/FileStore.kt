package nl.mpcjanssen.simpletask.remote

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.writeToFile
import java.io.File
import java.io.FilenameFilter
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object FileStore : FileStoreInterface {
    override fun getVersion(filename: String): String {
        return File(filename).lastModified().toString()
    }

    override fun needsRefresh(currentVersion: String?): String? {
        try {
            val lastModified = Config.todoFile.lastModified().toString()
            if (lastModified == currentVersion) {
                return null
            } else {
                return lastModified
            }
        } catch (e: Throwable) {
            log.error(TAG, "Can't determine if refresh is needed.", e)
            return null
        }

    }

    override val isOnline = true
    private val TAG = "FileStore"
    private val log: Logger
    private var observer: TodoObserver? = null


    init {
        log = Logger
        log.info(TAG, "onCreate")
        log.info(TAG, "Default path: ${getDefaultPath()}")
        observer = null

    }

    override val isAuthenticated: Boolean
        get() = true

    override fun loadTasksFromFile(path: String, eol: String): RemoteContents {
        log.info(TAG, "Loading tasks")
        val result = CopyOnWriteArrayList<String>()
        val completeFile = ArrayList<String>()
        val file = File(path)
        val lines = file.readLines()
        log.info(TAG, "Read ${lines.size} lines from $path")
        for (line in lines) {
            completeFile.add(line)
            result.add(line)
        }
        setWatching(path)
        return RemoteContents(file.lastModified().toString(), lines)
    }

    override fun sync() {
        log.info(TAG, "Sync.")
        broadcastFileSync(TodoApplication.app.localBroadCastManager)
    }

    override fun writeFile(file: File, contents: String) {
        log.info(TAG, "Writing file to  ${file.canonicalPath}")
        file.writeText(contents)
    }

    override fun readFile(file: String, fileRead: FileStoreInterface.FileReadListener?): String {
        log.info(TAG, "Reading file: {}" + file)
        val contents: String
        val lines = File(file).readLines()
        contents = join(lines, "\n")
        fileRead?.fileRead(contents)
        return contents
    }

    override fun supportsSync(): Boolean {
        return true
    }

    override fun startLogin(caller: Activity) {
        // FIXME possible add permission retrieval on Lollipop here

    }

    private fun setWatching(path: String) {
        Logger.info(TAG, "Observer: adding folder watcher on ${File(path).parentFile.absolutePath}")
        val obs = observer
        if (obs != null && path == obs.path) {
            Logger.warn(TAG, "Observer: already watching: $path")
            return
        } else if (obs != null) {
            Logger.warn(TAG, "Observer: already watching different path: ${obs.path}")
            obs.ignoreEvents(true)
            obs.stopWatching()
        }
        observer = TodoObserver(path)
        Logger.info(TAG, "Observer: modifying done")
    }

    override fun saveTasksToFile(path: String, lines: List<String>, eol: String): String {
        log.info(TAG, "Saving tasks to file: $path")
        val obs = observer
        obs?.ignoreEvents(true)
        val file = File(path)
        writeToFile(lines, eol, file, false)
        obs?.delayedStartListen(1000)
        return file.lastModified().toString()
    }

    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        log.info(TAG, "Appending ${lines.size} tasks to $path")
        writeToFile(lines, eol, File(path), true)
    }

    override fun getWritePermission(act: Activity, activityResult: Int): Boolean {

        val permissionCheck = ContextCompat.checkSelfPermission(act,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(act,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), activityResult)
        }
        return permissionCheck == PackageManager.PERMISSION_GRANTED
    }

    override fun logout() {

    }

    override fun getDefaultPath(): String {
        return "${Environment.getExternalStorageDirectory()}/data/nl.mpcjanssen.simpletask/todo.txt"
    }

    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {
        val result = ArrayList<FileEntry>()
        val file = File(path)

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
        // Run the file filter for side effects
        file.list(filter)
        return result
    }

    class TodoObserver(val path: String) : FileObserver(File(path).parentFile.absolutePath) {
        private val TAG = "FileWatchService"
        private val fileName: String
        private var log = Logger
        private var ignoreEvents: Boolean = false
        private val handler: Handler

        private val delayedEnable = Runnable {
            log.info(TAG, "Observer: Delayed enabling events for: " + path)
            ignoreEvents(false)
        }

        init {
            this.startWatching()
            this.fileName = File(path).name
            log.info(TAG, "Observer: creating observer on: {}")
            this.ignoreEvents = false
            this.handler = Handler(Looper.getMainLooper())

        }

        fun ignoreEvents(ignore: Boolean) {
            log.info(TAG, "Observer: observing events on " + this.path + "? ignoreEvents: " + ignore)
            this.ignoreEvents = ignore
        }

        override fun onEvent(event: Int, eventPath: String?) {
            if (eventPath != null && eventPath == fileName) {
                log.debug(TAG, "Observer event: $path:$event")
                if (event == FileObserver.CLOSE_WRITE ||
                        event == FileObserver.MODIFY ||
                        event == FileObserver.MOVED_TO) {
                    if (ignoreEvents) {
                        log.info(TAG, "Observer: ignored event on: " + path)
                    } else {
                        log.info(TAG, "File changed {}" + path)
                        FileStore.remoteTodoFileChanged()
                    }
                }
            }

        }

        fun delayedStartListen(ms: Int) {
            // Cancel any running timers
            handler.removeCallbacks(delayedEnable)
            // Reschedule
            Logger.info(TAG, "Observer: Adding delayed enabling to queue")
            handler.postDelayed(delayedEnable, ms.toLong())
        }

    }
}
