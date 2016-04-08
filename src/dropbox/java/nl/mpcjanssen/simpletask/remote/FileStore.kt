package nl.mpcjanssen.simpletask.remote


import android.app.*
import android.content.*
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.dropbox.client2.DropboxAPI
import com.dropbox.client2.RESTUtility
import com.dropbox.client2.android.AndroidAuthSession
import com.dropbox.client2.exception.DropboxException
import com.dropbox.client2.exception.DropboxIOException
import com.dropbox.client2.exception.DropboxServerException
import com.dropbox.client2.exception.DropboxUnlinkedException
import com.dropbox.client2.jsonextract.JsonExtractionException
import com.dropbox.client2.jsonextract.JsonThing
import com.dropbox.client2.session.AppKeyPair
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.*
import java.io.*
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList


/**
 * FileStore implementation backed by Dropbox
 */
class FileStore(private val mApp: TodoApplication, private val mFileChangedListerer: FileStoreInterface.FileChangeListener) : FileStoreInterface {

    private val log: Logger
    private val mPrefs: SharedPreferences?
    // In the class declaration section:
    private var mDBApi: DropboxAPI<AndroidAuthSession>? = null

    internal var onOnline: Thread? = null
    override var isLoading = false
    private var mOnline: Boolean = false
    private var fileOperationsQueue: Handler? = null

    init {
        log = Logger
        mPrefs = mApp.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
        // Set up the message queue
        val t = Thread(Runnable {
            Looper.prepare()
            fileOperationsQueue = Handler()
            Looper.loop()
        })
        t.start()
        mOnline = isOnline
        setMDBApi(mApp)
        RefreshAlarm.scheduleRefresh(mApp)
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
        edit.putString(LOCAL_REVISION, rev)
        edit.commit()
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
        edit.putBoolean(LOCAL_CHANGES_PENDING, pending).commit()
        mApp.localBroadCastManager.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_PENDING_CHANGES))
    }

    override val isOnline: Boolean
        get() {
            val cm = mApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            return netInfo != null && netInfo.isConnected
        }

    private val localTodoRev: String?
        get() {
            if (mPrefs == null) {
                return ""
            }
            return mPrefs.getString(LOCAL_REVISION, null)
        }

    private fun setMDBApi(app: TodoApplication) {
        val app_secret: String
        var app_key: String
        if (mDBApi == null) {
            // Full access or folder access?
            if (app.fullDropBoxAccess) {
                app_secret = mApp.getString(R.string.dropbox_consumer_secret)
                app_key = mApp.getString(R.string.dropbox_consumer_key)
            } else {
                app_secret = mApp.getString(R.string.dropbox_folder_consumer_secret)
                app_key = mApp.getString(R.string.dropbox_folder_consumer_key)
            }
            app_key = app_key.replaceFirst("^db-".toRegex(), "")
            // And later in some initialization function:
            val appKeys = AppKeyPair(app_key, app_secret)
            val savedAuth = mPrefs!!.getString(OAUTH2_TOKEN, null)
            val session = AndroidAuthSession(appKeys, savedAuth)
            mDBApi = DropboxAPI(session)
        }
    }


    // Required to complete auth, sets the access token on the session
    override val isAuthenticated: Boolean
        get() {
            if (mDBApi == null) {
                return false
            }
            if (mDBApi!!.session.isLinked) {
                return true
            }
            if (mDBApi!!.session.authenticationSuccessful()) {
                try {
                    mDBApi!!.session.finishAuthentication()
                    val accessToken = mDBApi!!.session.oAuth2AccessToken
                    mPrefs!!.edit().putString(OAUTH2_TOKEN, accessToken).commit()
                    return true
                } catch (e: IllegalStateException) {
                    log.info(TAG, "Error authenticating", e)
                }

            }
            return false
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
            return tasks
        } else {
            try {
                val openFileStream: DropboxAPI.DropboxInputStream
                val fileInfo: DropboxAPI.DropboxFileInfo
                try {
                    openFileStream = mDBApi!!.getFileStream(path, null)
                    fileInfo = openFileStream.fileInfo
                    log.info(TAG, "The file's rev is: " + fileInfo.metadata.rev)
                } catch (e: DropboxServerException) {
                    log.debug(TAG, "Dropbox server exception", e)
                    if (e.error == DropboxServerException._404_NOT_FOUND) {
                        log.info(TAG, "File not found, creating file instead")
                        val toStore = "".toByteArray()
                        val `in` = ByteArrayInputStream(toStore)
                        mDBApi!!.putFile(path, `in`,
                                toStore.size.toLong(), null, null)
                        openFileStream = mDBApi!!.getFileStream(path, null)
                        fileInfo = openFileStream.fileInfo
                    } else {
                        throw e
                    }
                }

                val reader = BufferedReader(InputStreamReader(openFileStream, "UTF-8"))

                reader.forEachLine { line ->
                    readFile.add(line)
                }
                openFileStream.close()
                val contents = join(readFile, "\n")
                backup?.backup(path, contents)
                saveToCache(fileInfo.metadata.fileName(), fileInfo.metadata.rev, contents)
            } catch (e: DropboxException) {
                // Couldn't download file use cached version
                e.printStackTrace()
                readFile.clear()
                readFile.addAll(tasksFromCache())
            } catch (e: IOException) {
                isLoading = false
                throw IOException(e)
            }

        }
        isLoading = false
        return readFile
    }

    private fun tasksFromCache(): List<String> {
        val result = CopyOnWriteArrayList<String>()
        val contents = loadContentsFromCache()
        for (line in contents.split("(\r\n|\r|\n)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            result.add(line)
        }
        return result
    }

    override fun startLogin(caller: Activity) {
        // MyActivity below should be your activity class name
        val intent = Intent(caller, LoginScreen::class.java)
        caller.startActivity(intent)
    }

    override fun logout() {
        if (mDBApi != null) {
            mDBApi!!.session.unlink()
        }
        mPrefs!!.edit().remove(OAUTH2_TOKEN).commit()
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
    override fun saveTasksToFile(path: String, lines: List<String>, backup: BackupInterface?, eol: String) {
        backup?.backup(path, join(lines, "\n"))
        val contents = join(lines, eol) + eol
        val r = Runnable {
            var newName = path
            var rev = localTodoRev
            try {
                val toStore = contents.toByteArray(charset("UTF-8"))
                val `in` = ByteArrayInputStream(toStore)
                log.info(TAG, "Saving to file " + path)
                val newEntry = mDBApi!!.putFile(path, `in`,
                        toStore.size.toLong(), rev, null)
                rev = newEntry.rev
                newName = newEntry.path
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
                mFileChangedListerer.fileChanged(newName)
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
                var rev: String? = null
                // First read file to append to
                try {
                    val openFileStream = mDBApi!!.getFileStream(path, null)
                    val fileInfo = openFileStream.fileInfo
                    rev = fileInfo.metadata.rev
                    log.info(TAG, "The file's rev is: " + fileInfo.metadata.rev)
                    val reader = BufferedReader(InputStreamReader(openFileStream, "UTF-8"))

                    reader.forEachLine { line ->
                        doneContents.add(line)
                    }
                    openFileStream.close()

                } catch (e: DropboxException) {
                    log.info(TAG, "Couldn't read " + path)
                }

                // Then append
                for (t in lines) {
                    doneContents.add(t)
                }
                val toStore = (join(doneContents, eol) + eol).toByteArray(charset("UTF-8"))
                val `in` = ByteArrayInputStream(toStore)

                mDBApi!!.putFile(path, `in`,
                        toStore.size.toLong(), rev, null)
                `in`.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        queueRunnable("Append to file " + path, r)
    }

    override fun sync() {
        mFileChangedListerer.fileChanged(null)
    }

    override fun writeFile(file: File, contents: String) {
        if (!isAuthenticated) {
            log.error(TAG, "Not authenticated, file ${file.canonicalPath} not written.")
            return
        }
        val toStore = contents.toByteArray(charset("UTF-8"))
        val r = Runnable {
            val inStream = ByteArrayInputStream(toStore)
            mDBApi?.putFileOverwrite(file.canonicalPath, inStream, toStore.size.toLong(), null)
            inStream.close()
        }
        queueRunnable("Write to file ${file.canonicalPath}", r)
    }

    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: FileStoreInterface.FileReadListener?): String {
        if (!isAuthenticated) {
            return ""
        }
        isLoading = true
        try {
            val openFileStream = mDBApi!!.getFileStream(file, null)
            val fileInfo = openFileStream.fileInfo
            log.info(TAG, "The file's rev is: " + fileInfo.metadata.rev)

            val reader = BufferedReader(InputStreamReader(openFileStream, "UTF-8"))
            val readFile = ArrayList<String>()
            reader.forEachLine { line ->
                readFile.add(line)
            }
            openFileStream.close()
            val contents = join(readFile, "\n")
            fileRead?.fileRead(contents)
            return contents
        } catch (e: DropboxException) {
            throw IOException(e)
        } finally {
            isLoading = false
        }
    }

    override fun supportsSync(): Boolean {
        return true
    }

    override val type: Int
        get() = Constants.STORE_DROPBOX


    fun changedConnectionState() {
        val prevOnline = mOnline
        mOnline = isOnline
        if (!prevOnline && mOnline) {
            // Schedule a task to reload the file
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
                        mFileChangedListerer.fileChanged(null)
                    } else {

                        log.info(TAG, "Device no longer online skipping reload")
                    }
                })
            }

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
     * @param pathName intial path shown in the file dialog
     */
    class FileDialog (private val activity: Activity, pathName: String, private val txtOnly: Boolean) {
        private val log: Logger
        private var fileList: Array<String>? = null
        private val entryHash = HashMap<String, DropboxAPI.Entry>()
        private var currentPath: File? = null

        private val fileListenerList = ListenerList<FileStoreInterface.FileSelectedListener>()
        internal var dialog: Dialog? = null
        private var loadingOverlay: Dialog? = null


        init {
            log = Logger
            currentPath = File(pathName)

        }

        /**

         */
        fun createFileDialog(act: Activity, fs: FileStoreInterface) {
            loadingOverlay = showLoadingOverlay(act, null, true)

            val api = (fs as FileStore).mDBApi ?: return

            // Use an asynctask because we need to manage the UI
            Thread(Runnable {
                loadFileList(act, api, currentPath?:File("/"))
                loadingOverlay = showLoadingOverlay(act, loadingOverlay, false)
                runOnMainThread(Runnable {
                    val builder = AlertDialog.Builder(activity)
                    builder.setTitle(currentPath!!.path)

                    builder.setItems(fileList, DialogInterface.OnClickListener { dialog, which ->
                        val fileChosen = fileList!![which]
                        if (fileChosen == PARENT_DIR) {
                            currentPath = File(currentPath!!.parent)
                            createFileDialog(act, fs)
                            return@OnClickListener
                        }
                        val chosenFile = getChosenFile(fileChosen)
                        log.warn(TAG, "Selected file " + chosenFile.name)
                        val entry = entryHash[fileChosen]
                        if (entry == null) {
                            dialog.dismiss()
                            return@OnClickListener
                        }
                        if (entry.isDir) {
                            currentPath = chosenFile
                            createFileDialog(act, fs)
                        } else {
                            dialog.cancel()
                            dialog.dismiss()
                            fireFileSelectedEvent(chosenFile)
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


        private fun fireFileSelectedEvent(file: File) {
            fileListenerList.fireEvent(object : ListenerList.FireHandler<FileStoreInterface.FileSelectedListener> {
                override fun fireEvent(listener: FileStoreInterface.FileSelectedListener) {
                    listener.fileSelected(file.toString())
                }
            })
        }

        @Throws(DropboxException::class)
        private fun getPathMetaData(api: DropboxAPI<AndroidAuthSession>?, path: File): DropboxAPI.Entry? {
            if (api != null) {
                return api.metadata(path.toString(), 0, null, true, null)
            } else {
                return null
            }
        }

        private fun loadFileList(act: Activity, api: DropboxAPI<AndroidAuthSession>, path: File) {
            this.currentPath = path
            val f = ArrayList<String>()
            val d = ArrayList<String>()

            try {
                val entries = getPathMetaData(api, path)
                entryHash.clear()
                if (entries == null) {
                    return
                }
                if (!entries.isDir) return
                if (path.toString() != "/") {
                    d.add(PARENT_DIR)
                }
                for (entry in entries.contents) {
                    if (entry.isDeleted) continue
                    if (entry.isDir) {
                        d.add(entry.fileName())
                    } else {
                        if (txtOnly) {
                            if (!File(entry.fileName()).extension.equals("txt")) {
                                continue
                            }
                        }
                        f.add(entry.fileName())
                    }
                    entryHash.put(entry.fileName(), entry)
                }
            } catch (e: DropboxException) {
                log.warn(TAG, "Couldn't load list from " + path.name + " loading root instead.")
                loadFileList(act, api, File("/"))
                return
            }

            Collections.sort(d)
            Collections.sort(f)
            d.addAll(f)
            fileList = d.toArray<String>(arrayOfNulls<String>(d.size))
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

    companion object {

        val TAG = "FileStoreDB"

        private val LOCAL_CONTENTS = "localContents"
        private val LOCAL_NAME = "localName"
        private val LOCAL_CHANGES_PENDING = "localChangesPending"
        private val LOCAL_REVISION = "localRev"
        private val CACHE_PREFS = "dropboxMeta"
        private val OAUTH2_TOKEN = "dropboxToken"

        fun getDefaultPath(app: TodoApplication): String {
            if (app.fullDropBoxAccess) {
                return "/todo/todo.txt"
            } else {
                return "/todo.txt"
            }
        }
    }
}

class RefreshAlarm () : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val m_app = context?.applicationContext as TodoApplication
        log.info("RefreshAlarm", "Scheduled update from dropbox.")
        m_app?.fileStore.sync()
        scheduleRefresh(m_app)
    }

    companion object {
        // Schedule refreshes
        fun scheduleRefresh(app: TodoApplication) {
            val interval = TodoApplication.prefs.getString(app.getString(R.string.dropbox_refresh_period), "300").toInt()
            Logger.info(FileStore.TAG, "Scheduling next tasklist refresh after $interval seconds")
            val intent = Intent(app, RefreshAlarm::class.java)
            val pi = PendingIntent.getBroadcast(app, 0, intent, 0)
            val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (interval>0) {
                am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + interval * 1000, pi)
            }
        }
    }
}