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
import nl.mpcjanssen.simpletask.task.TodoList.queue
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

    private val TAG = "FileStore"

    private val log: Logger = Logger

    internal var onOnline: Thread? = null
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

    override fun needsRefresh(currentVersion: String?): String? {
        return try {
            val remoteVersion = getVersion(Config.todoFileName)
            log.info(TAG, "Cached version ${Config.lastSeenRemoteId}, remote version $remoteVersion.")
            if (remoteVersion == currentVersion) null else remoteVersion
        } catch (e: Exception) {
            Logger.error(TAG, "Can't determine if refresh is needed.", e)
            null
        }
    }

    override fun getVersion(filename: String): String {
        val op = ReadRemoteFileOperation(filename)
        val res = op.execute(mNextcloud)
        val file = res.data[0] as RemoteFile
        return file.etag
    }

    override val isOnline: Boolean
        get() {
            val cm = mApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            return netInfo != null && netInfo.isConnected
        }

    override fun loadTasksFromFile(path: String, eol: String): RemoteContents {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we try to upload local

        log.info(TAG, "Loading file from Nextcloud: " + path)
        if (!isAuthenticated) {
            throw IOException("Not authenticated")
        }
        val readLines = ArrayList<String>()

        val cacheDir = mApp.applicationContext.cacheDir
        val op = DownloadRemoteFileOperation(path, cacheDir.canonicalPath)
        op.execute(mNextcloud)
        val infoOp = ReadRemoteFileOperation(path)
        val res = infoOp.execute(mNextcloud)
        if (res.httpCode == 404) {
            throw (IOException("File not found"))
        }
        val fileInfo = res.data[0] as RemoteFile
        val cachePath = File(cacheDir, path).canonicalPath
        readLines.addAll(File(cachePath).readLines())
        val currentVersionId = fileInfo.etag
        return RemoteContents(currentVersionId, readLines)
    }


    override fun startLogin(caller: Activity) {
        // MyActivity below should be your activity class name
        val intent = Intent(caller, LoginScreen::class.java)
        caller.startActivity(intent)
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<String>, eol: String): String {
        val contents = join(lines, eol) + eol

        val timestamp = timeStamp()

        log.info(TAG, "Saving to file " + path)
        val cacheDir = mApp.applicationContext.cacheDir
        val tmpFile = File(cacheDir, "tmp.txt")
        tmpFile.writeText(contents)

        // if we have previously seen a file from the server, we don't upload unless it's the
        // one we've seen before. If we've never seen a file, we just upload unconditionally
        val res = UploadRemoteFileOperation(tmpFile.absolutePath, path,
                "text/plain", timestamp).execute(mNextcloud)

        val conflict = 412
        if (res.httpCode == conflict) {
            val file = File(path)
            val parent = file.parent
            val name = file.name
            val nameWithoutTxt = "\\.txt$".toRegex().replace(name, "")
            val newName = nameWithoutTxt + "_conflict_" + UUID.randomUUID() + ".txt"
            val newPath = parent + "/" + newName
            val op = UploadRemoteFileOperation(tmpFile.absolutePath, newPath,
                    "text/plain", timestamp).execute(mNextcloud)

            showToastLong(TodoApplication.app, "CONFLICT! Uploaded as " + newName
                    + ". Review differences manually with a text editor.")
        }

        val infoOp = ReadRemoteFileOperation(path)
        val infoRes = infoOp.execute(mNextcloud)
        val fileInfo = infoRes.data[0] as RemoteFile

        return fileInfo.etag

    }


    @Throws(IOException::class)
    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        if (!isOnline) {
            throw IOException("Device is offline")
        }


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


    }

    override fun sync() {
        broadcastFileSync(mApp.localBroadCastManager)
    }

    override fun writeFile(file: File, contents: String) {
        if (!isAuthenticated) {
            log.error(TAG, "Not authenticated, file ${file.canonicalPath} not written.")
            throw IOException("Not authenticated")
        }

        val cacheDir = mApp.applicationContext.cacheDir
        val tmpFile = File(cacheDir, "tmp.txt")
        tmpFile.writeText(contents)
        val op = UploadRemoteFileOperation(tmpFile.absolutePath, file.canonicalPath, "text/plain", timeStamp())
        val res = op.execute(mNextcloud)

    }

    private fun timeStamp() = (System.currentTimeMillis() / 1000).toString()

    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: FileStoreInterface.FileReadListener?): String {
        if (!isAuthenticated) {
            return ""
        }
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
        mApp.localBroadCastManager.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_UI))
        if (mOnline) {
            log.info(TAG, "Device went online")
        } else {
            log.info(TAG, "Device no longer online skipping reloadLuaConfig")
        }
    }

    override fun getWritePermission(act: Activity, activityResult: Int): Boolean {
        return true
    }


    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {
        val result = ArrayList<FileEntry>()
        val op = ReadRemoteFolderOperation(File(path).canonicalPath)
        val res: RemoteOperationResult = op.execute(mNextcloud)
        // Loop over the resulting files
        // Drop the first one as it is the current folder
        res.data.drop(1).forEach { file ->
            if (file is RemoteFile) {
                val filename = File(file.remotePath).name
                result.add(FileEntry(filename, isFolder = (file.mimeType == "DIR")))
            }

        }
        return result
    }

    override fun getDefaultPath(): String {
        return "/todo.txt"
    }
}
