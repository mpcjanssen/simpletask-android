package nl.mpcjanssen.simpletask.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.showToastLong
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.reflect.KClass

private val s1 = System.currentTimeMillis().toString()

/**
 * FileStore implementation backed by Nextcloud
 */
object FileStore : IFileStore {

    internal val NEXTCLOUD_USER = "ncUser"
    internal val NEXTCLOUD_PASS = "ncPass"
    internal val NEXTCLOUD_URL = "ncURL"

    var username by TodoApplication.config.StringOrNullPreference(NEXTCLOUD_USER)
    var password by TodoApplication.config.StringOrNullPreference(NEXTCLOUD_PASS)
    var serverUrl by TodoApplication.config.StringOrNullPreference(NEXTCLOUD_URL)

    override val isAuthenticated: Boolean
        get() {
            Log.d("FileStore", "FileStore is authenticated ${username != null}")
            return username != null
        }

    override fun logout() {
        username = null
        password = null
        serverUrl = null
    }

    private val TAG = "FileStore"

    private var mOnline: Boolean = false

    private val mApp = TodoApplication.app

    private fun getClient () : OwnCloudClient? {
        serverUrl?.let { url ->
            val ctx = TodoApplication.app.applicationContext
            OwnCloudClientManagerFactory.setUserAgent("Mozilla/5.0 (Android) Nextcloud-android/3.8.1")
            val client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(url), ctx, true)
            client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                    username,
                    password
            )
            return client
        }
        return null
    }


    override fun getRemoteVersion(filename: String): String {
        val op = ReadFileRemoteOperation(filename)
        val res = op.execute(getClient())
        val file = res.data[0] as RemoteFile
        Log.d(TAG, "Remote versions of $filename: id: ${file.remoteId} tag: ${file.etag} modified: ${file.modifiedTimestamp} ")
        return file.etag
    }

    override val isOnline: Boolean
        get() {
            val cm = mApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            val online =  netInfo != null && netInfo.isConnected
            Log.d("FileStore","Filestore online: $online")
            return online
        }

    override fun loadTasksFromFile(path: String): RemoteContents {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we try to upload local

        Log.i(TAG, "Loading file from Nextcloud: " + path)
        if (!isAuthenticated) {
            throw IOException("Not authenticated")
        }
        val readLines = ArrayList<String>()

        val cacheDir = mApp.applicationContext.cacheDir
        val op = DownloadFileRemoteOperation(path, cacheDir.canonicalPath)
        val client = getClient()
        op.execute(client)
        val infoOp = ReadFileRemoteOperation(path)
        val res = infoOp.execute(client)
        if (res.httpCode == 404) {
            throw (IOException("File not found"))
        }
        val fileInfo = res.data[0] as RemoteFile
        val cachePath = File(cacheDir, path).canonicalPath
        readLines.addAll(File(cachePath).readLines())
        val currentVersionId = fileInfo.etag
        return RemoteContents(currentVersionId, readLines)
    }


    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<String>, lastRemote: String?, eol: String) : String {
        val contents = join(lines, eol) + eol

        val timestamp = timeStamp()

        Log.i(TAG, "Saving to file " + path)
        val cacheDir = mApp.applicationContext.cacheDir
        val tmpFile = File(cacheDir, "tmp.txt")
        tmpFile.writeText(contents)
        val client = getClient()
        // if we have previously seen a file from the server, we don't upload unless it's the
        // one we've seen before. If we've never seen a file, we just upload unconditionally
        val res = UploadFileRemoteOperation(tmpFile.absolutePath, path,
                "text/plain", timestamp).execute(client)

        val conflict = 412
        if (res.httpCode == conflict) {
            val file = File(path)
            val parent = file.parent
            val name = file.name
            val nameWithoutTxt = "\\.txt$".toRegex().replace(name, "")
            val newName = nameWithoutTxt + "_conflict_" + UUID.randomUUID() + ".txt"
            val newPath = parent + "/" + newName
            UploadFileRemoteOperation(tmpFile.absolutePath, newPath,
                    "text/plain", timestamp).execute(client)

            showToastLong(TodoApplication.app, "CONFLICT! Uploaded as " + newName
                    + ". Review differences manually with a text editor.")
        }

        val infoOp = ReadFileRemoteOperation(path)
        val infoRes = infoOp.execute(client)
        val fileInfo = infoRes.data[0] as RemoteFile
        Log.i(TAG,"New remote version tag: ${fileInfo.etag}, id: ${fileInfo.remoteId}, modified: ${fileInfo.modifiedTimestamp}")
        return fileInfo.etag

    }

    @Throws(IOException::class)
    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        if (!isOnline) {
            throw IOException("Device is offline")
        }
        val cacheDir = mApp.applicationContext.cacheDir
        val client = getClient()
        val op = DownloadFileRemoteOperation(path, cacheDir.canonicalPath)
        val result = op.execute(client)
        val doneContents = if (result.isSuccess) {
            val cachePath = File(cacheDir, path).canonicalPath
            File(cachePath).readLines().toMutableList()
        } else {
            ArrayList<String>()
        }

        doneContents.addAll(lines)
        val contents = join(doneContents, eol) + eol

        val tmpFile = File(cacheDir, "tmp.txt")
        tmpFile.writeText(contents)
        val timestamp = timeStamp()
        val writeOp = UploadFileRemoteOperation(tmpFile.absolutePath, path, "text/plain", timestamp)
        writeOp.execute(client)


    }

    override fun writeFile(path: String, contents: String) {
        if (!isAuthenticated) {
            Log.e(TAG, "Not authenticated, file $path not written.")
            throw IOException("Not authenticated")
        }

        val cacheDir = mApp.applicationContext.cacheDir
        val tmpFile = File(cacheDir, "tmp.txt")
        tmpFile.writeText(contents)
        val op = UploadFileRemoteOperation(tmpFile.absolutePath, path, "text/plain", timeStamp())
        val result = op.execute(getClient())
        Log.i(TAG, "Wrote file to $path, result ${result.isSuccess}")

    }

    private fun timeStamp() = (System.currentTimeMillis() / 1000).toString()

    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: (String) -> Unit) {
        if (!isAuthenticated) {
            return
        }
        val cacheDir = mApp.applicationContext.cacheDir
        val op = DownloadFileRemoteOperation(file, cacheDir.canonicalPath)
        op.execute(getClient())
        val cachePath = File(cacheDir, file).canonicalPath
        val contents = File(cachePath).readText()
        fileRead(contents)
    }


    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {
        val result = ArrayList<FileEntry>()
        val op = ReadFolderRemoteOperation(File(path).canonicalPath)
        val res: RemoteOperationResult = op.execute(getClient())
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
