package nl.mpcjanssen.simpletask.remote

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.InvalidAccessTokenException
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DownloadErrorException
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.remote.IFileStore.Companion.ROOT_DIR
import nl.mpcjanssen.simpletask.util.*
import java.io.*
import kotlin.reflect.KClass

/**
 * FileStore implementation backed by Dropbox
 * Dropbox V2 API docs suck, most of the V2 code was inspired by https://www.sitepoint.com/adding-the-dropbox-api-to-an-android-app/
 */
object FileStore : IFileStore {

    private val TAG = "FileStore"
    private val OAUTH2_TOKEN = "dropboxV2Token"

    private val mApp = TodoApplication.app

    var accessToken by TodoApplication.config.StringOrNullPreference(OAUTH2_TOKEN)

    var _dbxClient: DbxClientV2? = null

    val dbxClient: DbxClientV2
        get() {
            val newclient = _dbxClient ?: initDbxClient()
            _dbxClient = newclient
            return newclient
        }

    private fun initDbxClient(): DbxClientV2 {
        val requestConfig = DbxRequestConfig.newBuilder("simpletask").build()
        return DbxClientV2(requestConfig, accessToken)
    }

    override val isAuthenticated: Boolean
        get() {
            val token = accessToken
            if (token == null) {
                return false
            } else {
                FileStoreActionQueue.add ("Verify token") {
                    try {
                        val accountMail = dbxClient.users().currentAccount.email
                        Log.d(TAG, "Authenticated for $accountMail")
                    } catch (e: InvalidAccessTokenException) {
                        Log.w(TAG, "Invalid access token")
                        accessToken = null
                        broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
                    } catch (e: DbxException) {
                        Log.w(TAG, "Dropbox API error", e)
                    }
                }
                return true // for now
            }
        }

    override fun logout() {
        _dbxClient?.auth()?.tokenRevoke()
        _dbxClient = null
        accessToken = null
    }

    override fun getRemoteVersion(filename: String): String {
        try {
            val data = dbxClient.files().getMetadata(filename) as FileMetadata
            return data.rev
        } catch (e: InvalidAccessTokenException) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            accessToken = null
            return ""
        }
    }

    override val isOnline: Boolean
        get() {
            val cm = mApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            return netInfo != null && netInfo.isConnected
        }

    override fun loadTasksFromFile(path: String): RemoteContents {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we upload local and handle any conflicts
        // on the dropbox side.

        Log.i(TAG, "Loading file from Dropbox: " + path)
        if (!isAuthenticated) {
            throw IOException("Not authenticated")
        }
        val readLines = ArrayList<String>()

        val download = dbxClient.files().download(path)
        val openFileStream = download.inputStream
        val fileInfo = download.result
        Log.i(TAG, "The file's rev is: " + fileInfo.rev)

        val reader = BufferedReader(InputStreamReader(openFileStream, "UTF-8"))

        reader.forEachLine { line ->
            readLines.add(line)
        }
        openFileStream.close()
        return RemoteContents(remoteId = fileInfo.rev, contents = readLines)
    }

    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<String>, lastSeen: String?, eol: String) : String {
        Log.i(TAG, "Saving ${lines.size} tasks to Dropbox.")
        val contents = join(lines, eol) + eol

        val rev = lastSeen
        Log.i(TAG, "Last seen rev $rev")

        val toStore = contents.toByteArray(charset("UTF-8"))
        val `in` = ByteArrayInputStream(toStore)
        Log.i(TAG, "Saving to file $path")
        val uploadBuilder = dbxClient.files().uploadBuilder(path)
        uploadBuilder.withAutorename(true).withMode(if (rev != null) WriteMode.update(rev) else null)
        val uploaded = try {
             uploadBuilder.uploadAndFinish(`in`)
        }  finally {
            `in`.close()
        }
        Log.i(TAG, "New rev " + uploaded.rev)
        val newName = uploaded.pathDisplay

        if (newName != path) {
            // The file was written under another name
            // Usually this means the was a conflict.
            Log.i(TAG, "Filename was changed remotely. New name is: $newName")
            showToastLong(mApp, "Filename was changed remotely. New name is: $newName")
            mApp.switchTodoFile(newName)
        }
        return uploaded.rev
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
            val currentRev = download.result.rev
            Log.i(TAG, "The file's rev is: $currentRev")
            currentRev
        } catch (e: DownloadErrorException) {
            Log.i(TAG, "$path doesn't exist. Creating instead of appending")
            null
        }
        // Then append
        doneContents += lines
        val toStore = (join(doneContents, eol) + eol).toByteArray(charset("UTF-8"))
        val `in` = ByteArrayInputStream(toStore)
        dbxClient.files().uploadBuilder(path).withAutorename(true).withMode(if (rev != null) WriteMode.update(rev) else null).uploadAndFinish(`in`)
    }

    override fun writeFile(path: String, contents: String) {
        if (!isAuthenticated) {
            Log.e(TAG, "Not authenticated, file ${path} not written.")
            return
        }
        val toStore = contents.toByteArray(charset("UTF-8"))
        Log.d(TAG, "Write to file ${path}")
        val inStream = ByteArrayInputStream(toStore)
        dbxClient.files().uploadBuilder(path).withMode(WriteMode.OVERWRITE).uploadAndFinish(inStream)
    }

    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: (String) -> Unit) {
        if (!isAuthenticated) {
            return
        }

        val download = dbxClient.files().download(file)
        Log.i(TAG, "The file's rev is: " + download.result.rev)

        val reader = BufferedReader(InputStreamReader(download.inputStream, "UTF-8"))
        val readFile = ArrayList<String>()
        reader.forEachLine { line ->
            readFile.add(line)
        }
        download.inputStream.close()
        val contents = join(readFile, "\n")
        fileRead(contents)
    }

    override fun getDefaultPath(): String {
        return if (TodoApplication.config.fullDropBoxAccess) {
            "/todo/todo.txt"
        } else {
            "/todo.txt"
        }
    }

    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {

        val fileList = ArrayList<FileEntry>()

        try {
            val dbxPath = if (path == ROOT_DIR) "" else path
            val entries = FileStore.dbxClient.files().listFolder(dbxPath).entries
            entries?.forEach { entry ->
                if (entry is FolderMetadata)
                    fileList.add(FileEntry(entry.name, isFolder = true))
                else if (!txtOnly || File(entry.name).extension == "txt") {
                    fileList.add(FileEntry(entry.name, isFolder = false))
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Couldn't load file list, ", e)
        }
        return fileList
    }
}
