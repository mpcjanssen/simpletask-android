package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import nl.mpcjanssen.simpletask.Constants
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.*
import java.io.*
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * FileStore implementation backed by Dropbox
 * Dropbox V2 API docs suck, most of the V2 code was inspired by https://www.sitepoint.com/adding-the-dropbox-api-to-an-android-app/
 */
object FileStore : FileStoreInterface {

    private val TAG = "FileStoreDB"
    private val LOCAL_CONTENTS = "localContents"
    private val LOCAL_NAME = "localName"
    private val LOCAL_CHANGES_PENDING = "localChangesPending"
    private val CACHE_PREFS = "dropboxMeta"
    private val OAUTH2_TOKEN = "dropboxV2Token"

    private val log: Logger = Logger
    private val mPrefs: SharedPreferences?

    internal var onOnline: Thread? = null
    override var isLoading = false
    private var mOnline: Boolean = false
    private var fileOperationsQueue: Handler? = null
    private val mApp = TodoApplication.app

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
    
    val dbxClient by lazy {
        val accessToken = getAccessToken()
        val requestConfig = DbxRequestConfig.newBuilder("simpletask").build()
        val client = DbxClientV2(requestConfig, accessToken)
        client
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

    fun getAccessToken(): String? {
        val accessToken = mPrefs?.getString(OAUTH2_TOKEN, null)
        return accessToken
    }

    fun setAccessToken(accessToken: String?) {
        val edit = mPrefs?.edit()
        edit?.let {
            if (accessToken == null) {
                edit.remove(OAUTH2_TOKEN).apply()
            } else {
                edit.putString(OAUTH2_TOKEN, accessToken).apply()
            }
        }
    }

    override val isAuthenticated: Boolean
        get() = getAccessToken() != null

    override fun logout() {
        setAccessToken(null)
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
        val data = dbxClient.files().getMetadata(filename) as FileMetadata
        return data.rev
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
        // our local changes, instead we upload local and handle any conflicts
        // on the dropbox side.

        log.info(TAG, "Loading file fom dropnbox: " + path)
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
            val download = dbxClient.files().download(path)
            val openFileStream = download.inputStream
            val fileInfo = download.result
            log.info(TAG, "The file's rev is: " + fileInfo.rev)

            val reader = BufferedReader(InputStreamReader(openFileStream, "UTF-8"))

            reader.forEachLine { line ->
                readFile.add(line)
            }
            openFileStream.close()
            val contents = join(readFile, "\n")
            backup?.backup(path, contents)
            saveToCache(fileInfo.name, fileInfo.rev, contents)
            Config.currentVersionId = fileInfo.rev
            startWatching(path)
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
            var newName = path
            var rev = Config.currentVersionId
            try {
                val toStore = contents.toByteArray(charset("UTF-8"))
                val `in` = ByteArrayInputStream(toStore)
                log.info(TAG, "Saving to file " + path)
                val uploadBuilder = dbxClient.files().uploadBuilder(path)
                if (rev != null) {
                    uploadBuilder.withAutorename(true).withMode(WriteMode.update(rev))
                }
                val uploaded = uploadBuilder.uploadAndFinish(`in`)
                rev = uploaded.rev
                newName = uploaded.pathDisplay
                if (updateVersion) {
                    Config.currentVersionId = rev
                }
                setChangesPending(false)
            } catch (e: Exception) {
                e.printStackTrace()
                // Changes are pending
                setChangesPending(true)
            } finally {
                // Always save to cache  so you wont lose changes
                // if actual save fails (e.g. when the device is offline)
                saveToCache(path, rev, contents)
            }

            if (newName != path) {
                // The file was written under another name
                // Usually this means the was a conflict.
                log.info(TAG, "Filename was changed remotely. New name is: " + newName)
                showToastLong(mApp, "Filename was changed remotely. New name is: " + newName)
                mApp.switchTodoFile(newName)
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

                val doneContents = ArrayList<String>()
                val download = dbxClient.files().download(path)
                download.inputStream.bufferedReader().forEachLine {
                    doneContents.add(it)
                }
                val rev = download.result.rev
                log.info(TAG, "The file's rev is: " + rev)
                download.close()

                // Then append
                doneContents += lines
                val toStore = (join(doneContents, eol) + eol).toByteArray(charset("UTF-8"))
                val `in` = ByteArrayInputStream(toStore)
                dbxClient.files().uploadBuilder(path).withAutorename(true).withMode(WriteMode.update(rev)).uploadAndFinish(`in`)
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
        val toStore = contents.toByteArray(charset("UTF-8"))
        val r = Runnable {
            val inStream = ByteArrayInputStream(toStore)
            dbxClient.files().uploadBuilder(file.path).withMode(WriteMode.OVERWRITE).uploadAndFinish(`inStream`)
        }
        queueRunnable("Write to file ${file.canonicalPath}", r)
    }

    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: FileStoreInterface.FileReadListener?): String {
        if (!isAuthenticated) {
            return ""
        }
        isLoading = true

        val download = dbxClient.files().download(file)
        log.info(TAG, "The file's rev is: " + download.result.rev)

        val reader = BufferedReader(InputStreamReader(download.inputStream, "UTF-8"))
        val readFile = ArrayList<String>()
        reader.forEachLine { line ->
            readFile.add(line)
        }
        download.inputStream.close()
        val contents = join(readFile, "\n")
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
        private val entryHash = HashMap<String, com.dropbox.core.v2.files.Metadata>()
        private var currentPath = File(pathName)

        private val fileListenerList = ListenerList<FileStoreInterface.FileSelectedListener>()
        internal var dialog: Dialog? = null
        private var loadingOverlay: Dialog? = null

        /**

         */
        fun createFileDialog(act: Activity, fs: FileStoreInterface) {
            loadingOverlay = showLoadingOverlay(act, null, true)

            val api = (fs as FileStore).dbxClient

            // Use an async task because we need to manage the UI
            Thread(Runnable {
                loadFileList(act, api, currentPath)
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
                        log.warn(TAG, "Selected file " + entry?.pathDisplay)
                        if (entry == null) {
                            dialog.dismiss()
                            return@OnClickListener
                        }
                        if (entry is FolderMetadata) {
                            currentPath = File(entry.pathDisplay)
                            createFileDialog(act, fs)
                        } else {
                            dialog.cancel()
                            dialog.dismiss()
                            fireFileSelectedEvent(entry.pathDisplay)
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

        private fun loadFileList(act: Activity, api: DbxClientV2, path: File) {
            this.currentPath = path
            val f = ArrayList<String>()
            val d = ArrayList<String>()

            entryHash.clear()
            try {
                val dbxPath = if (path.canonicalPath == "/") "" else path.canonicalPath
                if (dbxPath != "") {
                    d.add(PARENT_DIR)
                }

                val entries = dbxClient.files().listFolder(dbxPath).entries
                entries?.forEach {
                    entry ->
                    if (entry is FolderMetadata)
                        d.add(entry.name)
                    else if (txtOnly) {
                        if (File(entry.name).extension == "txt") {
                            f.add(entry.name)
                        }
                    } else {
                        f.add(entry.name)
                    }
                    entryHash.put(entry.name, entry)
                }

                Collections.sort(d)
                Collections.sort(f)
                d.addAll(f)
                fileList = d.toArray<String>(arrayOfNulls<String>(d.size))
            } catch (e: DbxException) {
                Logger.error(TAG, "Dropbox error:", e)
                loadFileList(act, api, File("/"))
                return
            }
        }

        companion object {
            private val PARENT_DIR = ".."
        }
    }

    fun getDefaultPath(): String {
        if (Config.fullDropBoxAccess) {
            return "/todo/todo.txt"
        } else {
            return "/todo.txt"
        }
    }
}
