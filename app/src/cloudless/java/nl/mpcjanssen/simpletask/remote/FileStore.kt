package nl.mpcjanssen.simpletask.remote

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.Constants
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.*
import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

object FileStore : FileStoreInterface {
    private val mApp = TodoApplication.app
    override fun getVersion(filename: String): String {
        return File(filename).lastModified().toString()
    }

    override fun needsRefresh(currentVersion: String?): Boolean {
        val lastModified = Config.todoFile.lastModified()
        if (lastModified == 0L ) {
            return true
        }
        return currentVersion?.toLong() ?: 0 < lastModified
    }

    override val isOnline = true
    private val TAG = javaClass.simpleName
    private val bm: LocalBroadcastManager
    private val log: Logger
    private var observer: TodoObserver? = null

    override var isLoading: Boolean = false

    private var fileOperationsQueue: Handler? = null

    init {
        log = Logger
        log.info(TAG, "onCreate")
        log.info(TAG, "Default path: ${getDefaultPath()}")
        observer = null
        this.bm = LocalBroadcastManager.getInstance(TodoApplication.app)

        // Set up the message queue
        val t = Thread(Runnable {
            Looper.prepare()
            fileOperationsQueue = Handler()
            Looper.loop()
        })
        t.start()
    }

    override val isAuthenticated: Boolean
        get() = true

    fun queueRunnable(description: String, r: Runnable) {
        log.info(TAG, "Handler: Queue " + description)
        while (fileOperationsQueue == null) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

        }
        fileOperationsQueue!!.post(r)
    }

    override fun loadTasksFromFile(path: String, backup: BackupInterface?, eol: String): List<String> {
        log.info(TAG, "Loading tasks")
        val result = CopyOnWriteArrayList<String>()
        isLoading = true
        try {
            val completeFile = ArrayList<String>()
            val lines = File(path).readLines()
            log.info(TAG, "Read ${lines.size} lines from $path")
            for (line in lines) {
                completeFile.add(line)
                result.add(line)
            }
            backup?.backup(path, join(completeFile, "\n"))
        } catch (e: IOException) {
            log.info(TAG, "Read read failed", e)
            e.printStackTrace()
        } finally {
            isLoading = false
        }
        setWatching(path)
        return result
    }

    override fun sync() {
        broadcastFileChanged(mApp.localBroadCastManager)
    }

    override fun writeFile(file: File, contents: String) {
        log.info(TAG, "Writing file to  ${file.canonicalPath}")
        file.writeText(contents)
    }

    override fun readFile(file: String, fileRead: FileStoreInterface.FileReadListener?): String {
        log.info(TAG, "Reading file: {}" + file)
        isLoading = true
        val contents: String
        val lines = File(file).readLines()
        contents = join(lines, "\n")
        isLoading = false
        fileRead?.fileRead(contents)
        return contents
    }

    override fun supportsSync(): Boolean {
        return true
    }

    override fun changesPending(): Boolean {
        return false
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

    override fun browseForNewFile(act: Activity, path: String, listener: FileStoreInterface.FileSelectedListener, txtOnly: Boolean) {
        val dialog = FileDialog(act, path, txtOnly)
        dialog.addFileListener(listener)
        dialog.createFileDialog(act, this)
    }

    @Synchronized override fun saveTasksToFile(path: String, lines: List<String>, backup: BackupInterface?, eol: String, updateVersion: Boolean) {
        log.info(TAG, "Saving tasks to file: {}" + path)

        queueRunnable("Save ${lines.size} lines to file " + path, Runnable {
            backup?.backup(path, join(lines, "\n"))
            val obs = observer
            obs?.ignoreEvents(true)
            try {
                writeToFile(lines, eol, File(path), false)
                if (updateVersion) {
                    Config.currentVersionId = File(path).lastModified().toString()
                }

            } catch (e: IOException) {
                log.error(TAG, "Saving $path failed", e)
                e.printStackTrace()
            } finally {
                obs?.delayedStartListen(1000)
            }
        })

    }

    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        queueRunnable("Appending  ${lines.size} lines tasks to $path", Runnable {
            log.info(TAG, "Appending ${lines.size} tasks to $path")
            try {
                writeToFile(lines, eol, File(path), true)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            bm.sendBroadcast(Intent(Constants.BROADCAST_SYNC_DONE))
        })
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

    class FileDialog
    /**
     * @param activity
     * *
     * @param pathName
     */
    (private val activity: Activity, pathName: String, private val txtOnly: Boolean) {
        private var fileList: Array<String>? = null
        private var currentPath: File? = null

        private val fileListenerList = ListenerList<FileStoreInterface.FileSelectedListener>()

        init {
            var path = File(pathName)
            if (!path.exists() || !path.isDirectory) path = Environment.getExternalStorageDirectory()
            loadFileList(path)
        }

        /**
         * @return file dialog
         */

        // Parameters are needed for dropbox version
        fun createFileDialog(@Suppress("UNUSED_PARAMETER") ctx: Context?, @Suppress("UNUSED_PARAMETER") fs: FileStoreInterface?): Dialog {
            val dialog: Dialog
            val builder = AlertDialog.Builder(activity)

            builder.setTitle(currentPath!!.path)

            builder.setItems(fileList) { dialog, which ->
                val fileChosen = fileList!![which]
                val chosenFile = getChosenFile(fileChosen)
                if (chosenFile.isDirectory) {
                    loadFileList(chosenFile)
                    dialog.cancel()
                    dialog.dismiss()
                    showDialog()
                } else
                    fireFileSelectedEvent(chosenFile)
            }

            dialog = builder.show()
            return dialog
        }

        fun addFileListener(listener: FileStoreInterface.FileSelectedListener) {
            fileListenerList.add(listener)
        }

        /**
         * Show file dialog
         */
        fun showDialog() {
            createFileDialog(null, null).show()
        }

        private fun fireFileSelectedEvent(file: File) {
            fileListenerList.fireEvent(object : ListenerList.FireHandler<FileStoreInterface.FileSelectedListener> {
                override fun fireEvent(listener: FileStoreInterface.FileSelectedListener) {
                    listener.fileSelected(file.toString())
                }
            })
        }

        private fun loadFileList(path: File) {
            this.currentPath = path
            val r = ArrayList<String>()
            if (path.exists()) {
                if (path.parentFile != null) r.add(PARENT_DIR)
                val filter = FilenameFilter { dir, filename ->
                    val sel = File(dir, filename)
                    if (!sel.canRead())
                        false
                    else {
                        val txtFile = filename.toLowerCase(Locale.getDefault()).endsWith(".txt")
                        !txtOnly || sel.isDirectory || txtFile
                    }
                }
                val fileList1 = path.list(filter)
                if (fileList1 != null) {
                    Collections.addAll(r, *fileList1)
                } else {
                    // Fallback to root
                    r.add("/")
                }
            } else {
                // Fallback to root
                r.add("/")
            }
            Collections.sort(r)
            fileList = r.toArray(arrayOfNulls<String>(r.size))
        }

        private fun getChosenFile(fileChosen: String): File {
            if (fileChosen == PARENT_DIR)
                return currentPath!!.parentFile
            else
                return File(currentPath, fileChosen)
        }

        companion object {
            private val PARENT_DIR = ".."
        }
    }

    override fun logout() {

    }
    fun getDefaultPath(): String {
        return "${Environment.getExternalStorageDirectory()}/data/nl.mpcjanssen.simpletask/todo.txt"
    }
}

class TodoObserver(val path: String) : FileObserver(File(path).parentFile.absolutePath) {
    private val TAG = "FileWatchService"
    private val bm: LocalBroadcastManager = LocalBroadcastManager.getInstance(TodoApplication.app)
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
                    broadcastFileChanged(bm)
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
