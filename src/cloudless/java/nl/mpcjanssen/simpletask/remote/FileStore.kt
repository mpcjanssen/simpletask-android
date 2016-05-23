package nl.mpcjanssen.simpletask.remote

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.os.FileObserver
import android.os.Handler
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.Constants
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.ListenerList
import nl.mpcjanssen.simpletask.util.*


import java.io.File
import java.io.FilenameFilter
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

class FileStore(ctx: Context, private val m_fileChangedListener: FileStoreInterface.FileChangeListener) : FileStoreInterface {
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
        observer = null
        this.bm = LocalBroadcastManager.getInstance(ctx)

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

    @Synchronized override fun loadTasksFromFile(path: String, backup: BackupInterface?, eol: String): List<String> {
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

    }

    override fun writeFile(file: File, contents: String ) {
        log.info(TAG, "Writing file to  ${file.canonicalPath}")
        file.writeText(contents)
    }

    override fun readFile(file: String, fileRead: FileStoreInterface.FileReadListener?): String {
        log.info(TAG, "Reading file: {}" + file)
        isLoading = true
        var contents : String
        val lines = File(file).readLines()
        contents = join(lines, "\n")
        isLoading = false
        fileRead?.fileRead(contents)
        return contents
    }

    override fun supportsSync(): Boolean {
        return false
    }

    override fun changesPending(): Boolean {
        return false
    }

    override fun startLogin(caller: Activity) {
        // FIXME possible add permission retrieval on Lollipop here

    }

    @Synchronized private fun setWatching(path: String) {

        log.info(TAG, "Observer: adding {} " + File(path).parentFile.absolutePath)
        val obs = observer
        if (obs != null && path == obs.path) {
            log.warn(TAG, "Observer: already watching: {}")
            return
        } else if (obs != null) {
            log.warn(TAG, "Observer: already watching different path: {}" + obs.path)
            obs.ignoreEvents(true)
            obs.stopWatching()
        }
        observer = TodoObserver(path, m_fileChangedListener)
        log.info(TAG, "Observer: modifying done")
    }

    override fun browseForNewFile(act: Activity, path: String, listener: FileStoreInterface.FileSelectedListener, showTxt: Boolean) {
        val dialog = FileDialog(act, path, showTxt)
        dialog.addFileListener(listener)
        dialog.createFileDialog(act, this)
    }

    @Synchronized override fun saveTasksToFile(path: String, lines: List<String>, backup: BackupInterface?, eol: String) {
        log.info(TAG, "Saving tasks to file: {}" + path)
        backup?.backup(path, join(lines, "\n"))
        val obs = observer
        obs?.ignoreEvents(true)

        queueRunnable("Save ${lines.size} lines to file " + path, Runnable {
            try {
                writeToFile(join(lines, eol) + eol, File(path), false)
            } catch (e: IOException) {
                log.error(TAG, "Saving $path failed" ,e)
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
                writeToFile(join(lines, eol) + eol, File(path), true)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            bm.sendBroadcast(Intent(Constants.BROADCAST_SYNC_DONE))
        })
    }

    override val type: Int
        get() = Constants.STORE_SDCARD

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
        fun createFileDialog(ctx: Context?, fs: FileStoreInterface?): Dialog {
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
            fileList = r.toArray<String>(arrayOfNulls<String>(r.size))
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

    private inner class TodoObserver(val path: String, private val fileChangedListener: FileStoreInterface.FileChangeListener) : FileObserver(File(path).parentFile.absolutePath) {
        private val fileName: String
        private var log =  Logger
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
                        fileChangedListener.fileChanged(path)
                    }
                }
            }

        }

        fun delayedStartListen(ms: Int) {
            // Cancel any running timers
            handler.removeCallbacks(delayedEnable)
            // Reschedule
            log.info(TAG, "Observer: Adding delayed enabling to queue")
            handler.postDelayed(delayedEnable, ms.toLong())
        }
    }

    companion object {

        fun getDefaultPath(app: TodoApplication): String {
            return "${Environment.getExternalStorageDirectory()}/data/nl.mpcjanssen.simpletask/todo.txt"
        }
    }
}
