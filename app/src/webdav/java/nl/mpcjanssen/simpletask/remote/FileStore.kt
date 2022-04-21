package nl.mpcjanssen.simpletask.remote

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log

import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import nl.mpcjanssen.simpletask.R

import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.TodoException
import nl.mpcjanssen.simpletask.util.broadcastAuthFailed
import nl.mpcjanssen.simpletask.util.join
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.reflect.KClass

private val s1 = System.currentTimeMillis().toString()

/**
 * FileStore implementation backed by Webdav
 */
object FileStore : IFileStore {


    internal val USER = "remoteUser"
    internal val PASS = "remotePass"
    internal val URL = "remoteURL"
    private var lastSeenRemoteId by TodoApplication.config.StringOrNullPreference(R.string.file_current_version_id)

    var username by TodoApplication.config.StringOrNullPreference(USER)
    var password by TodoApplication.config.StringOrNullPreference(PASS)
    var serverUrl by TodoApplication.config.StringOrNullPreference(URL)

    private fun url(file: File) = (serverUrl?.trimEnd('/') + file.canonicalPath).trimEnd('/')
    override val isEncrypted: Boolean
        get() = false

    val isAuthenticated: Boolean
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

    private val mApp = TodoApplication.app

    private fun getClient () : Sardine? {
        serverUrl?.let { url ->
            return OkHttpSardine().also { it.setCredentials(username, password) }
        }
        return null
    }

    private fun DavResource.remoteVersion() : String {
        return "${this.modified}:${this.etag}"
    }



    private fun getRemoteVersion(file: File): String {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return ""
        }
        getClient()?.let {
            val url = url(file)
            val res = it.list(url)[0]
            Log.i(TAG, "Getting metadata for ${url}, etag: ${res.etag}, modified: ${res.modified}")
            return res.remoteVersion()

        } ?: throw TodoException("WebDav exception client is null")
    }

    override val isOnline: Boolean
        get() {
            val cm = mApp.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo
            val online =  netInfo != null && netInfo.isConnected
            Log.d("FileStore","Filestore online: $online")
            return online
        }

    override fun loadTasksFromFile(file: File): List<String> {


        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we try to upload local
        Log.i(TAG, "Loading file from: ${url(file)}")
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return emptyList()
        }
        getClient()?.let {
            val readLines = it.get(url(file)).bufferedReader(Charsets.UTF_8).readLines()
            lastSeenRemoteId = getRemoteVersion(file)
            return readLines
        }?:  throw TodoException("WebDav exception client is null")
    }

    override fun needSync(file: File): Boolean {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return true
        }
        return getRemoteVersion(file)!= lastSeenRemoteId
    }

    override fun todoNameChanged() {
        lastSeenRemoteId = ""
    }


    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(file: File, lines: List<String>, eol: String) : File {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return file
        }
        getClient()?.let { client ->
            Log.i(TAG, "Uploading file to ${url(file)}")
            val contents = join(lines, eol) + eol
            val remoteVersion = getRemoteVersion(file)
            val savedFile = if (lastSeenRemoteId == null || remoteVersion == lastSeenRemoteId) {
                client.put(url(file), contents.toByteArray(Charsets.UTF_8))
                file
            } else {
                Log.i(TAG,"File conflict remote: $remoteVersion, last seen: $lastSeenRemoteId ")
                val fileWithoutTxt = file.nameWithoutExtension
                val newFile = File(fileWithoutTxt + "_conflict_" + UUID.randomUUID() + ".txt")
                client.put(url(newFile), contents.toByteArray(Charsets.UTF_8))
                newFile
            }
            lastSeenRemoteId = getRemoteVersion(savedFile)
            return savedFile
        } ?:  throw TodoException("WebDav exception client is null")
    }

    @Throws(IOException::class)
    override fun appendTaskToFile(file: File, lines: List<String>, eol: String) {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return
        }
        getClient()?.let { client ->
            Log.i(TAG, "Appending to file ${url(file)}")
            val newLines = client.get(url(file)).bufferedReader(Charsets.UTF_8).readLines().toMutableList()
            newLines.addAll(lines)
            val contents = join(newLines, eol) + eol
            client.put(url(file), contents.toByteArray(Charsets.UTF_8))
        } ?:  throw TodoException("WebDav exception client is null")


    }

    override fun writeFile(file: File, contents: String) {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return
        }
        getClient()?.let { client ->
            Log.i(TAG, "Writing file to ${url(file)}")
            client.put(url(file), contents.toByteArray(Charsets.UTF_8))
        } ?:  throw TodoException("WebDav exception client is null")
    }

    private fun timeStamp() = (System.currentTimeMillis() / 1000).toString()

    @Throws(IOException::class)
    override fun readFile(file: File, fileRead: (String) -> Unit) {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return
        }
        getClient()?.let { client ->
            Log.i(TAG, "Writing file to ${url(file)}")
            fileRead.invoke(client.get(url(file)).bufferedReader(Charsets.UTF_8).readText())
        } ?:  throw TodoException("WebDav exception client is null")
    }


    override fun loadFileList(file: File, txtOnly: Boolean): List<FileEntry> {
        if (!isAuthenticated) {
            broadcastAuthFailed(TodoApplication.app.localBroadCastManager)
            return emptyList()
        }
        getClient()?.let { client ->
            val url = url(file) + "/"
            val result = ArrayList<FileEntry>()
            val resList = client.list(url)
            // Loop over the resulting files
            // Drop the first one as it is the current folder
            resList.forEach { res ->
                val fileItem = File(res.name)
                result.add(FileEntry(fileItem, res.isDirectory))
            }
            return result
        } ?:  throw TodoException("WebDav exception client is null")
    }


    override fun getDefaultFile(): File {
        return File("/todo.txt")
    }
}
