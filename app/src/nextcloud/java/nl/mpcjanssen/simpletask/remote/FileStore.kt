package nl.mpcjanssen.simpletask.remote

import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.*
import nl.mpcjanssen.simpletask.Constants
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.*
import java.io.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

private val s1 = System.currentTimeMillis().toString()

/**
 * FileStore implementation backed by Nextcloud
 */
object FileStore : FileStoreInterface {
    override val isAuthenticated: Boolean
        get() = !AccountManager.get(mApp.applicationContext).getAccountsByType(getString(R.string.account_type)).isEmpty()

    override fun logout() {
        val am = AccountManager.get(mApp.applicationContext)
        am.getAccountsByType(getString((R.string.account_type))).forEach {
            am.removeAccount(it, null, null)
        }
    }

    private val TAG = "FileStoreDB"
    private val LOCAL_CONTENTS = "localContents"
    private val LOCAL_NAME = "localName"
    private val LOCAL_CHANGES_PENDING = "localChangesPending"
    private val CACHE_PREFS = "nextcloudMeta"

    private val log: Logger = Logger
    private val mPrefs: SharedPreferences?

    internal var onOnline: Thread? = null
    override var isLoading = false
    private var mOnline: Boolean = false
    private var fileOperationsQueue: Handler? = null
    private val mApp = TodoApplication.app

    private val mNextcloud by lazy {
        val ctx = mApp.applicationContext
        val am = AccountManager.get(ctx)
        val accounts = am.getAccountsByType(getString((R.string.account_type)))
        val account = accounts[0]
        val password = am.getPassword(account)
        val server = am.getUserData(account, "server_url")
        val client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(server), ctx, true)
        client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                account.name,
                password
        )
        client
    }
    init {
        mPrefs = mApp.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
        // Set up the message queue
        val t = Thread(Runnable {
            Looper.prepare()
            fileOperationsQueue = Handler()
            Looper.loop()
        })
        t.start()
        mOnline = isOnline
    }

    override fun pause(pause: Boolean) {
        if (pause) {
            log.info(TAG, "App went to background stop watching")
            stopWatching()
        } else {
            log.info(TAG, "App came to foreground continue watching ${Config.todoFileName}")
            startWatching(Config.todoFileName)
        }
    }

    override fun needsRefresh(currentVersion: String?): Boolean {
        try {
            return getVersion(Config.todoFileName) != Config.currentVersionId
        } catch (e: Exception) {
            Logger.error(TAG, "Can't determine if refresh is needed.", e)
            return true
        }
    }

    override fun getVersion(filename: String): String {
        val op = ReadRemoteFileOperation(filename)
        val res = op.execute(mNextcloud)
        val file = res.data[0] as RemoteFile
        return file.etag
    }

    private fun loadContentsFromCache(): String {
        if (mPrefs == null) {
            log.warn(TAG, "Couldn't load cache from other_preferences, mPrefs == null")
            return ""
        }
        return mPrefs.getString(LOCAL_CONTENTS, "")
    }

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

    override fun changesPending(): Boolean {
        if (mPrefs == null) {
            log.error(TAG, "Couldn't read pending changes state, mPrefs == null")
            return false
        }
        return mPrefs.getBoolean(LOCAL_CHANGES_PENDING, false)
    }

    private fun saveToCache(fileName: String, rev: String?, contents: String) {
        log.info(TAG, "Storing file in cache rev: $rev of file: $fileName")
        if (mPrefs == null) {
            return
        }
        val edit = mPrefs.edit()
        edit.putString(LOCAL_NAME, fileName)
        edit.putString(LOCAL_CONTENTS, contents)
        edit.apply()
    }

    private fun setChangesPending(pending: Boolean) {
        if (mPrefs == null) {
            log.error(TAG, "Couldn't save pending changes, mPrefs == null")
            return
        }
        if (pending) {
            log.info(TAG, "Changes are pending")
        }
        val edit = mPrefs.edit()
        edit.putBoolean(LOCAL_CHANGES_PENDING, pending).apply()
        mApp.localBroadCastManager.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_PENDING_CHANGES))
    }

    override val isOnline: Boolean
        get() {
            val cm = mApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            return netInfo != null && netInfo.isConnected
        }

    @Synchronized @Throws(IOException::class)
    override fun loadTasksFromFile(path: String, backup: BackupInterface?, eol: String): List<String> {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we try to upload local

        log.info(TAG, "Loading file from Nextcloud: " + path)
        isLoading = true
        if (!isAuthenticated) {
            isLoading = false
            throw IOException("Not authenticated")
        }
        val readFile = ArrayList<String>()
        if (changesPending()) {
            log.info(TAG, "Not loading, changes pending")
            isLoading = false
            val tasks = tasksFromCache()
            saveTasksToFile(path, tasks, backup, eol)
            startWatching(path)
            return tasks
        } else {
            val cacheDir = mApp.applicationContext.cacheDir
            val op = DownloadRemoteFileOperation(path, cacheDir.canonicalPath)
            op.execute(mNextcloud)
            val infoOp = ReadRemoteFileOperation(path)
            val res = infoOp.execute(mNextcloud)
            if (res.httpCode == 404) {
                Config.currentVersionId = null
            } else {
                val fileInfo = res.data[0] as RemoteFile

                val cachePath = File(cacheDir, path).canonicalPath
                readFile.addAll(File(cachePath).readLines())

                val contents = join(readFile, "\n")
                backup?.backup(path, contents)
                saveToCache(fileInfo.remotePath, fileInfo.modifiedTimestamp.toString(), contents)
                Config.currentVersionId = fileInfo.modifiedTimestamp.toString()
                startWatching(path)
            }
        }

        return readFile
    }

    private fun tasksFromCache(): List<String> {
        val result = CopyOnWriteArrayList<String>()
        val contents = loadContentsFromCache()
        result += contents.split("(\r\n|\r|\n)".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        return result
    }

    override fun startLogin(caller: Activity) {
        // MyActivity below should be your activity class name
        val intent = Intent(caller, LoginScreen::class.java)
        caller.startActivity(intent)
    }

    private fun startWatching(path: String) {
        queueRunnable("Refresh", Runnable {
            if (needsRefresh(Config.currentVersionId)) {
                sync()
            }
        })
    }

    private fun stopWatching() {
        queueRunnable("stopWatching", Runnable {
        })
    }

    override fun browseForNewFile(act: Activity, path: String, listener: FileStoreInterface.FileSelectedListener, txtOnly: Boolean) {
        if (!isOnline) {
            showToastLong(mApp, "Device is offline")
            log.info(TAG, "Device is offline, browse closed")
            return
        }
        val dialog = FileDialog(act, path, true)
        dialog.addFileListener(listener)
        dialog.createFileDialog(act, this)
    }

    @Synchronized @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<String>, backup: BackupInterface?, eol: String, updateVersion: Boolean) {
        backup?.backup(path, join(lines, "\n"))
        val contents = join(lines, eol) + eol
        val r = Runnable {
            val timestamp = timeStamp()
            try {
                log.info(TAG, "Saving to file " + path)
                val cacheDir = mApp.applicationContext.cacheDir
                val tmpFile = File(cacheDir, "tmp.txt")
                tmpFile.writeText(contents)

                // if we have previously seen a file from the server, we don't upload unless it's the
                // one we've seen before. If we've never seen a file, we just upload unconditionally
                val op = if (Config.currentVersionId != null)
                    UploadRemoteFileOperation(tmpFile.absolutePath, path,
                            "text/plain", Config.currentVersionId, timestamp)
                else UploadRemoteFileOperation(tmpFile.absolutePath, path,
                        "text/plain", timestamp)

                val res = op.execute(mNextcloud)
                val CONFLICT = 412
                if (res.httpCode == 412) {
                    val file = File(path)
                    val parent = file.parent
                    val name = file.name
                    val nameWithoutTxt = "\\.txt$".toRegex().replace(name, "")
                    val newName = nameWithoutTxt + "_conflict_" + UUID.randomUUID() + ".txt"
                    val newPath = parent + "/" + newName
                    val op = UploadRemoteFileOperation(tmpFile.absolutePath, newPath,
                            "text/plain", timestamp)
                    val res = op.execute(mNextcloud)

                    Toast.makeText(mApp.applicationContext, "CONFLICT! Uploaded as " + newName
                            + ". Review differences manually with a text editor.", Toast.LENGTH_LONG).show()
                }

                val infoOp = ReadRemoteFileOperation(path)
                val infoRes = infoOp.execute(mNextcloud)
                val fileInfo = infoRes.data[0] as RemoteFile

                Config.currentVersionId = fileInfo.etag
                setChangesPending(false)
            } catch (e: Exception) {
                e.printStackTrace()
                // Changes are pending
                setChangesPending(true)
            } finally {
                // Always save to cache  so you wont lose changes
                // if actual save fails (e.g. when the device is offline)
                saveToCache(path, timestamp, contents)
                startWatching(path)
            }

        }
        queueRunnable("Save to file " + path, r)
    }

    @Throws(IOException::class)
    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        if (!isOnline) {
            throw IOException("Device is offline")
        }
        val r = Runnable {
            try {
                val cacheDir = mApp.applicationContext.cacheDir
                val op = DownloadRemoteFileOperation(path, cacheDir.canonicalPath)
                op.execute(mNextcloud)

                val cachePath = File(cacheDir, path).canonicalPath
                val originalLines = File(cachePath).readLines()

                val doneContents = ArrayList<String>()
                doneContents += originalLines
                doneContents += lines
                val contents = join(doneContents, eol) + eol

                val tmpFile = File(cacheDir, "tmp.txt")
                tmpFile.writeText(contents)
                val timestamp = timeStamp()
                val writeOp = UploadRemoteFileOperation(tmpFile.absolutePath, path, "text/plain", timestamp)
                val res = writeOp.execute(mNextcloud)
           } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        queueRunnable("Append to file " + path, r)
    }

    override fun sync() {
        broadcastFileChanged(mApp.localBroadCastManager)
    }

    override fun writeFile(file: File, contents: String) {
        if (!isAuthenticated) {
            log.error(TAG, "Not authenticated, file ${file.canonicalPath} not written.")
            return
        }
        val r = Runnable {
            val cacheDir = mApp.applicationContext.cacheDir
            val tmpFile = File(cacheDir, "tmp.txt")
            tmpFile.writeText(contents)
            val op = UploadRemoteFileOperation(tmpFile.absolutePath, file.canonicalPath, "text/plain", timeStamp())
            val res = op.execute(mNextcloud)
        }
        queueRunnable("Write to file ${file.canonicalPath}", r)
    }

    private fun timeStamp() = (System.currentTimeMillis() / 1000).toString()

    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: FileStoreInterface.FileReadListener?): String {
            if (!isAuthenticated) {
            return ""
        }
        isLoading = true
        val cacheDir = mApp.applicationContext.cacheDir
        val op = DownloadRemoteFileOperation(file, cacheDir.canonicalPath)
        op.execute(mNextcloud)
        val cachePath = File(cacheDir, file).canonicalPath
        val contents = File(cachePath).readText()
        fileRead?.fileRead(contents)
        return contents
    }

    override fun supportsSync(): Boolean {
        return true
    }

    fun changedConnectionState() {
        val prevOnline = mOnline
        mOnline = isOnline
        if (!prevOnline && mOnline) {
            // Schedule a task to reloadLuaConfig the file
            // Give some time to settle so we ignore rapid connectivity changes
            // Only schedule if another thread is not running
            if (onOnline == null || !onOnline!!.isAlive) {
                queueRunnable("onOnline", Runnable {
                    // Check if we are still online
                    log.info(TAG, "Device went online, reloading in 5 seconds")
                    try {
                        Thread.sleep(5000)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    if (isOnline) {
                        broadcastFileChanged(mApp.localBroadCastManager)
                    } else {

                        log.info(TAG, "Device no longer online skipping reloadLuaConfig")
                    }
                })
            }

        } else if (!mOnline) {
            stopWatching()
        }
        if (prevOnline && !mOnline) {
            mApp.localBroadCastManager.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_UI))
            log.info(TAG, "Device went offline")
        }
    }

    override fun getWritePermission(act: Activity, activityResult: Int): Boolean {
        return true
    }

    /**
     * @param activity activity to display the file dialog.
     * *
     * @param pathName initial path shown in the file dialog
     */
    class FileDialog(private val activity: Activity, pathName: String, private val txtOnly: Boolean) {
        private val log: Logger = Logger
        private var fileList: Array<String>? = null
        private val entryHash = HashMap<String, RemoteFile>()
        private var currentPath = File(pathName)

        private val fileListenerList = ListenerList<FileStoreInterface.FileSelectedListener>()
        internal var dialog: Dialog? = null
        private var loadingOverlay: Dialog? = null

        /**

         */
        fun createFileDialog(act: Activity, fs: FileStoreInterface) {
            loadingOverlay = showLoadingOverlay(act, null, true)

            // Use an async task because we need to manage the UI
            Thread(Runnable {
                loadFileList(act, currentPath)
                loadingOverlay = showLoadingOverlay(act, loadingOverlay, false)
                runOnMainThread(Runnable {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle(currentPath.canonicalPath)

                    builder.setItems(fileList, DialogInterface.OnClickListener { dialog, which ->
                        val fileChosen = fileList!![which]
                        if (fileChosen == PARENT_DIR) {
                            currentPath = currentPath.parentFile ?: File("/")
                            createFileDialog(act, fs)
                            return@OnClickListener
                        }
                        val entry = entryHash[fileChosen]
                        if (entry == null) {
                            dialog.dismiss()
                            return@OnClickListener
                        }
                        if (entry.mimeType == "DIR") {
                            currentPath = File(entry.remotePath)
                            createFileDialog(act, fs)
                        } else {
                            dialog.cancel()
                            dialog.dismiss()
                            fireFileSelectedEvent(entry.remotePath)
                        }
                    })
                    if (dialog != null && dialog!!.isShowing) {
                        dialog!!.cancel()
                        dialog!!.dismiss()
                    }
                    dialog = builder.create()
                    dialog!!.show()
                })
            }).start()
        }

        fun addFileListener(listener: FileStoreInterface.FileSelectedListener) {
            fileListenerList.add(listener)
        }

        private fun fireFileSelectedEvent(file: String) {
            fileListenerList.fireEvent(object : ListenerList.FireHandler<FileStoreInterface.FileSelectedListener> {
                override fun fireEvent(listener: FileStoreInterface.FileSelectedListener) {
                    listener.fileSelected(file)
                }
            })
        }

        fun loadFileList(act: Activity, path: File) {
            this.currentPath = path
            val f = ArrayList<String>()
            val d = ArrayList<String>()
            d.add(PARENT_DIR)

            entryHash.clear()
            try {
                val op = ReadRemoteFolderOperation(path.canonicalPath)
                val res: RemoteOperationResult = op.execute(mNextcloud)
                res.data.forEach({ file ->
                    if (file is RemoteFile && file.mimeType == "DIR") {
                        d.add(file.remotePath)
                        entryHash.put(file.remotePath, file)
                    }
                    if (file is RemoteFile && file.remotePath.endsWith(".txt")) {
                        f.add(file.remotePath)
                        entryHash.put(file.remotePath, file)
                    }

                })
                Collections.sort(d)
                Collections.sort(f)
                d.addAll(f)
                fileList = d.toArray<String>(arrayOfNulls<String>(d.size))
            } catch (e: Exception) {
                Logger.error(TAG, "Nextcloud error:", e)
                loadFileList(act, File("/"))
                return
            }
        }

        companion object {
            private val PARENT_DIR = ".."
        }
    }

    fun getDefaultPath(): String {
        return "/todo.txt"
    }
}
