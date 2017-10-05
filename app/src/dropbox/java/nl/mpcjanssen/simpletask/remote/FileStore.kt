package nl.mpcjanssen.simpletask.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DownloadErrorException
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import nl.mpcjanssen.simpletask.Constants
import nl.mpcjanssen.simpletask.Logger
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.remote.FileStoreInterface.Companion.PARENT_DIR
import nl.mpcjanssen.simpletask.remote.FileStoreInterface.Companion.ROOT_DIR
import nl.mpcjanssen.simpletask.remote.FileStoreInterface.FileEntry
import nl.mpcjanssen.simpletask.task.TodoList.queue
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.broadcastFileSync
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.showToastLong
import java.io.*

/**
 * FileStore implementation backed by Dropbox
 * Dropbox V2 API docs suck, most of the V2 code was inspired by https://www.sitepoint.com/adding-the-dropbox-api-to-an-android-app/
 */
object FileStore : FileStoreInterface {

    private val TAG = "FileStoreDB"
    private val LOCAL_CHANGES_PENDING = "localChangesPending"
    private val CACHE_PREFS = "dropboxMeta"
    private val OAUTH2_TOKEN = "dropboxV2Token"

    private val log: Logger = Logger
    private val mPrefs: SharedPreferences?

    internal var onOnline: Thread? = null
    override var isLoading = false
    private var mOnline: Boolean = false
    private val mApp = TodoApplication.app

    init {
        mPrefs = mApp.getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE)
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
            queue("Start watching") {
                startWatching(Config.todoFileName)
            }
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

    override fun needsRefresh(currentVersion: String?): String? {
        try {
            val remoteVersion = getVersion(Config.todoFileName)
            log.info(TAG, "Cached version ${Config.lastSeenRemoteId}, remote version ${remoteVersion}.")
            return if (remoteVersion == currentVersion) null else remoteVersion
        } catch (e: Exception) {
            log.error(TAG, "Can't determine if refresh is needed.", e)
            return null
        }
    }

    override fun getVersion(filename: String): String {
        val data = dbxClient.files().getMetadata(filename) as FileMetadata
        return data.rev
    }

    override fun changesPending(): Boolean {
        if (mPrefs == null) {
            log.error(TAG, "Couldn't read pending changes state, mPrefs == null")
            return false
        }
        return mPrefs.getBoolean(LOCAL_CHANGES_PENDING, false)
    }


    fun setChangesPending(pending: Boolean) {
        log.info(TAG, "Set changes pending.")
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

    @Synchronized
    @Throws(IOException::class)
    override fun loadTasksFromFile(path: String, eol: String): FileStoreInterface.RemoteContents {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we upload local and handle any conflicts
        // on the dropbox side.

        log.info(TAG, "Loading file fom Dropbox: " + path)
        isLoading = true
        if (!isAuthenticated) {
            isLoading = false
            throw IOException("Not authenticated")
        }
        val readLines = ArrayList<String>()

        val download = dbxClient.files().download(path)
        val openFileStream = download.inputStream
        val fileInfo = download.result
        log.info(TAG, "The file's rev is: " + fileInfo.rev)

        val reader = BufferedReader(InputStreamReader(openFileStream, "UTF-8"))

        reader.forEachLine { line ->
            readLines.add(line)
        }
        openFileStream.close()
        startWatching(path)
        return FileStoreInterface.RemoteContents(remoteId = fileInfo.rev, contents = readLines)
    }


    override fun startLogin(caller: Activity) {
        // MyActivity below should be your activity class name
        val intent = Intent(caller, LoginScreen::class.java)
        caller.startActivity(intent)
    }

    private fun startWatching(path: String) {
        if (needsRefresh(Config.lastSeenRemoteId) != null) {
            sync()
        }
    }

    private fun stopWatching() {

    }


    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<String>, eol: String): String {
        log.info(TAG, "Saving ${lines.size} tasks to Dropbox.")
        val contents = join(lines, eol)

        var newName = path
        var rev = Config.lastSeenRemoteId
        val toStore = contents.toByteArray(charset("UTF-8"))
        val `in` = ByteArrayInputStream(toStore)
        log.info(TAG, "Saving to file " + path)
        val uploadBuilder = dbxClient.files().uploadBuilder(path)
        uploadBuilder.withAutorename(true).withMode(if (rev != null) WriteMode.update(rev) else null)
        val uploaded = uploadBuilder.uploadAndFinish(`in`)
        rev = uploaded.rev
        newName = uploaded.pathDisplay
        setChangesPending(false)

        if (newName != path) {
            // The file was written under another name
            // Usually this means the was a conflict.
            log.info(TAG, "Filename was changed remotely. New name is: " + newName)
            showToastLong(mApp, "Filename was changed remotely. New name is: " + newName)
            mApp.switchTodoFile(newName)
        }
        return rev
    }

    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        if (!isOnline) {
            throw IOException("Device is offline")
        }

        val doneContents = ArrayList<String>()
        val rev = try {
            val download = dbxClient.files().download(path)
            download.inputStream.bufferedReader().forEachLine {
                doneContents.add(it)
            }
            download.close()
            download.result.rev
        } catch (e: DownloadErrorException) {
            log.info(TAG, "${path} doesn't seem to exist", e)
            null
        }
        log.info(TAG, "The file's rev is: " + rev)

        // Then append
        doneContents += lines
        val toStore = (join(doneContents, eol) + eol).toByteArray(charset("UTF-8"))
        val `in` = ByteArrayInputStream(toStore)
        dbxClient.files().uploadBuilder(path).withAutorename(true).withMode(if (rev != null) WriteMode.update(rev) else null).uploadAndFinish(`in`)
    }


    override fun sync() {
        log.info(TAG, "Sync.")
        broadcastFileSync(mApp.localBroadCastManager)
    }

    override fun writeFile(file: File, contents: String) {
        if (!isAuthenticated) {
            log.error(TAG, "Not authenticated, file ${file.canonicalPath} not written.")
            return
        }
        val toStore = contents.toByteArray(charset("UTF-8"))
        queue("Write to file ${file.canonicalPath}") {
            val inStream = ByteArrayInputStream(toStore)
            dbxClient.files().uploadBuilder(file.path).withMode(WriteMode.OVERWRITE).uploadAndFinish(`inStream`)
        }
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
                // Check if we are still online
                log.info(TAG, "Device went online, reloading in 5 seconds")
                try {
                    Thread.sleep(5000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

                if (isOnline) {
                    broadcastFileSync(mApp.localBroadCastManager)
                } else {

                    log.info(TAG, "Device no longer online skipping reloadLuaConfig")
                }
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

    override fun getDefaultPath(): String {
        if (Config.fullDropBoxAccess) {
            return "/todo/todo.txt"
        } else {
            return "/todo.txt"
        }
    }

    override fun loadFileList(path: String, txtOnly: Boolean): List<FileStoreInterface.FileEntry> {

        val fileList = ArrayList<FileEntry>()

        val dbxPath = if (path == ROOT_DIR) "" else path
        if (dbxPath != "") {
            fileList.add(FileEntry(PARENT_DIR, isFolder = true))
        }

        val entries = FileStore.dbxClient.files().listFolder(dbxPath).entries
        entries?.forEach { entry ->
            if (entry is FolderMetadata)
                fileList.add(FileEntry(entry.name, isFolder = true))
            else if (!txtOnly || File(entry.name).extension == "txt") {
                fileList.add(FileEntry(entry.name, isFolder = false))
            }
        }
        return fileList
    }
}
