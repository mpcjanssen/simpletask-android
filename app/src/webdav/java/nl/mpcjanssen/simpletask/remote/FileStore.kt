package nl.mpcjanssen.simpletask.remote

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log

import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine

import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.TodoException
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

    var username by TodoApplication.config.StringOrNullPreference(USER)
    var password by TodoApplication.config.StringOrNullPreference(PASS)
    var serverUrl by TodoApplication.config.StringOrNullPreference(URL)

    private fun url(file: File) = serverUrl?.trimEnd('/') + file.canonicalPath ?: ""

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

    private val mApp = TodoApplication.app

    private fun getClient () : Sardine? {
        serverUrl?.let { url ->
            return OkHttpSardine().also { it.setCredentials(username, password) }
        }
        return null
    }

    fun DavResource.remoteVersion() : String {
        return "${this.etag}:${this.modified}"
    }

    private fun getRemoteVersion(url: String): String {
        getClient()?.let {
            val res = it.list(url)[0]
            Log.e(TAG, "Getting metadata for ${url}, etag: ${res.etag}, modified: ${res.modified}")
            return res.remoteVersion()

        } ?: throw TodoException("WebDav exception client is null")
    }

    override fun getRemoteVersion(file: File): String {
        getClient()?.let {
            val res = it.list(url(file))[0]
            Log.e(TAG, "Getting metadata for ${url(file)}, etag: ${res.etag}, modified: ${res.modified}")
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

    override fun loadTasksFromFile(file: File): RemoteContents {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we try to upload local
        Log.i(TAG, "Loading file from: ${url(file)}")
        if (!isAuthenticated) {
            throw IOException("Not authenticated")
        }
        getClient()?.let {
            val version = getRemoteVersion(file)
            val readLines = it.get(url(file)).bufferedReader(Charsets.UTF_8).readLines()
            return RemoteContents(version, readLines)
        }?:  throw TodoException("WebDav exception client is null")
    }


    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(file: File, lines: List<String>, lastRemote: String?, eol: String) : String {
        getClient()?.let { client ->
            Log.i(TAG, "Uploading file to ${url(file)}")
            val contents = join(lines, eol) + eol
            val remoteVersion = getRemoteVersion(file)
            val fileUrl = url(file)
            val savedUrl = if (lastRemote == null || remoteVersion == lastRemote) {
                client.put(fileUrl, contents.toByteArray(Charsets.UTF_8))
                fileUrl
            } else {
                Log.i(TAG,"File conflict remote: $remoteVersion, last seen: $lastRemote ")
                val urlWithoutTxt = "\\.txt$".toRegex().replace(fileUrl, "")
                val newUrl = urlWithoutTxt + "_conflict_" + UUID.randomUUID() + ".txt"
                client.put(newUrl, contents.toByteArray(Charsets.UTF_8))
                newUrl
            }
            return getRemoteVersion(savedUrl)
        } ?:  throw TodoException("WebDav exception client is null")
    }

    @Throws(IOException::class)
    override fun appendTaskToFile(file: File, lines: List<String>, eol: String) {
        getClient()?.let { client ->
            Log.i(TAG, "Appending to file ${url(file)}")
            val newLines = client.get(url(file)).bufferedReader(Charsets.UTF_8).readLines().toMutableList()
            newLines.addAll(lines)
            val contents = join(newLines, eol) + eol
            client.put(url(file), contents.toByteArray(Charsets.UTF_8))
        } ?:  throw TodoException("WebDav exception client is null")


    }

    override fun writeFile(file: File, contents: String) {
        getClient()?.let { client ->
            Log.i(TAG, "Writing file to ${url(file)}")
            client.put(url(file), contents.toByteArray(Charsets.UTF_8))
        } ?:  throw TodoException("WebDav exception client is null")
    }

    private fun timeStamp() = (System.currentTimeMillis() / 1000).toString()

    @Throws(IOException::class)
    override fun readFile(file: File, fileRead: (String) -> Unit) {
        getClient()?.let { client ->
            Log.i(TAG, "Writing file to ${url(file)}")
            fileRead.invoke(client.get(url(file)).bufferedReader(Charsets.UTF_8).readText())
        } ?:  throw TodoException("WebDav exception client is null")
    }


    override fun loadFileList(file: File, txtOnly: Boolean): List<FileEntry> {
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
