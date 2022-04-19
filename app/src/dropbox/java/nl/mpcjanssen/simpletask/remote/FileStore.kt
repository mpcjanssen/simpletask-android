package nl.mpcjanssen.simpletask.remote

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.net.ConnectivityManager
import android.util.Log
import com.dropbox.core.DbxException
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.InvalidAccessTokenException
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.DownloadErrorException
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.WriteMode
import nl.mpcjanssen.simpletask.R
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
    fun clientIdentifier(): String {
        return if (TodoApplication.config.fullDropBoxAccess) {
            "SimpletaskAndroidFull/1.0.0"
        }   else {
            "SimpletaskAndroidFolder/1.0.0"
        }
    }
    private val TAG = "FileStore"

    private val mApp = TodoApplication.app

    private var lastSeenRemoteId by TodoApplication.config.StringOrNullPreference(R.string.file_current_version_id)
    private var _dbxClient: DbxClientV2? = null

    private val dbxClient: DbxClientV2
        get() {
            val newclient = _dbxClient ?: initDbxClient()
            _dbxClient = newclient
            return newclient
        }

    private fun getLocalCredential(): DbxCredential? {
        val sharedPreferences = mApp.getSharedPreferences("dropbox", MODE_PRIVATE)
        val serializedCredential = sharedPreferences.getString("credential", null) ?: return null
        return DbxCredential.Reader.readFully(serializedCredential)
    }

    private fun initDbxClient(): DbxClientV2 {
        //deserialize the credential from SharedPreferences if it exists
        val clientIdentifier = "DropboxSampleAndroid/1.0.0"
        val requestConfig = DbxRequestConfig(clientIdentifier)
        val credential = getLocalCredential()

        return DbxClientV2(requestConfig, credential)
    }

    override val isEncrypted: Boolean
        get() = false

    override val isAuthenticated: Boolean
        get() {
            val token = getLocalCredential()
            if (token == null) {
                return false
            } else {
                    try {
                        val accountMail = dbxClient.users().currentAccount.email
                        Log.d(TAG, "Authenticated for $accountMail")
                        return true
                    } catch (e: InvalidAccessTokenException) {
                        Log.w(TAG, "Invalid access token")
                        broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
                    } catch (e: DbxException) {
                        Log.w(TAG, "Dropbox API error", e)
                    }

                return false // for now
            }
        }

    override fun logout() {

            val requestConfig = DbxRequestConfig(clientIdentifier())
            val credential = getLocalCredential()
            val dropboxClient = DbxClientV2(requestConfig, credential)
            dropboxClient.auth().tokenRevoke()
            val sharedPreferences = mApp.getSharedPreferences("dropbox", MODE_PRIVATE)
            sharedPreferences.edit().remove("credential").apply()
            mApp.clearTodoFile()
    }


    override val isOnline: Boolean
        get() {
            val cm = mApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            return netInfo != null && netInfo.isConnected
        }

    override fun loadTasksFromFile(file: File): List<String> {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we upload local and handle any conflicts
        // on the dropbox side.

        Log.i(TAG, "Loading file from Dropbox: " + file)
        if (!isAuthenticated) {
            throw IOException("Not authenticated")
        }
        val readLines = ArrayList<String>()

        val download = dbxClient.files().download(file.canonicalPath)
        val openFileStream = download.inputStream
        val fileInfo = download.result
        Log.i(TAG, "The file's rev is: " + fileInfo.rev)
        lastSeenRemoteId = fileInfo.rev

        val reader = BufferedReader(InputStreamReader(openFileStream, "UTF-8"))

        reader.forEachLine { line ->
            readLines.add(line)
        }
        openFileStream.close()
        return readLines
    }

    override fun needSync(file: File): Boolean {
        try {
            val data = dbxClient.files().getMetadata(file.canonicalPath) as FileMetadata
            return data.rev != lastSeenRemoteId
        } catch (e: InvalidAccessTokenException) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return true
        }
    }

    override fun todoNameChanged() {
        lastSeenRemoteId = ""
    }


    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Throws(IOException::class)
    override fun saveTasksToFile(file: File, lines: List<String>, eol: String) : File {
        Log.i(TAG, "Saving ${lines.size} tasks to Dropbox.")
        val contents = join(lines, eol) + eol

        Log.i(TAG, "Last seen rev $lastSeenRemoteId")

        val toStore = contents.toByteArray(charset("UTF-8"))
        val `in` = ByteArrayInputStream(toStore)
        Log.i(TAG, "Saving to file $file")
        val uploadBuilder = dbxClient.files().uploadBuilder(file.canonicalPath)
        uploadBuilder.withAutorename(true).withMode(if (!lastSeenRemoteId.isNullOrBlank()) WriteMode.update(lastSeenRemoteId) else null)
        val uploaded = try {
             uploadBuilder.uploadAndFinish(`in`)
        }  finally {
            `in`.close()
        }
        Log.i(TAG, "New rev " + uploaded.rev)
        lastSeenRemoteId = uploaded.rev
        val newName = uploaded.pathDisplay

        return File(newName)
    }

    override fun appendTaskToFile(file: File, lines: List<String>, eol: String) {
        if (!isOnline) {
            throw IOException("Device is offline")
        }

        val doneContents = ArrayList<String>()
        val rev = try {
            val download = dbxClient.files().download(file.canonicalPath)
            download.inputStream.bufferedReader().forEachLine {
                doneContents.add(it)
            }
            download.close()
            val currentRev = download.result.rev
            Log.i(TAG, "The file's rev is: $currentRev")
            currentRev
        } catch (e: DownloadErrorException) {
            Log.i(TAG, "$file doesn't exist. Creating instead of appending")
            null
        }
        // Then append
        doneContents += lines
        val toStore = (join(doneContents, eol) + eol).toByteArray(charset("UTF-8"))
        val `in` = ByteArrayInputStream(toStore)
        dbxClient.files().uploadBuilder(file.canonicalPath).withAutorename(true).withMode(if (rev != null) WriteMode.update(rev) else null).uploadAndFinish(`in`)
    }

    override fun writeFile(file: File, contents: String) {
        if (!isAuthenticated) {
            Log.e(TAG, "Not authenticated, file ${file} not written.")
            return
        }
        val toStore = contents.toByteArray(charset("UTF-8"))
        Log.d(TAG, "Write to file ${file}")
        val inStream = ByteArrayInputStream(toStore)
        dbxClient.files().uploadBuilder(file.canonicalPath).withMode(WriteMode.OVERWRITE).uploadAndFinish(inStream)
    }

    @Throws(IOException::class)
    override fun readFile(file: File, fileRead: (String) -> Unit) {
        if (!isAuthenticated) {
            return
        }

        val download = dbxClient.files().download(file.canonicalPath)
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

    override fun getDefaultFile(): File {
        return if (TodoApplication.config.fullDropBoxAccess) {
            File("/todo/todo.txt")
        } else {
            File("/todo.txt")
        }
    }

    override fun loadFileList(file: File, txtOnly: Boolean): List<FileEntry> {

        val fileList = ArrayList<FileEntry>()

        try {
            val dbxPath = if (file.canonicalPath == ROOT_DIR) "" else file.canonicalPath
            val entries = FileStore.dbxClient.files().listFolder(dbxPath).entries
            entries?.forEach { entry ->
                if (entry is FolderMetadata)
                    fileList.add(FileEntry(File(entry.name), isFolder = true))
                else if (!txtOnly || File(entry.name).extension == "txt") {
                    fileList.add(FileEntry(File(entry.name), isFolder = false))
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Couldn't load file list, ", e)
        }
        return fileList
    }
}
