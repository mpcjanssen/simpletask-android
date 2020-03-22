package nl.mpcjanssen.simpletask.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.util.Log

import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import kotlinx.android.synthetic.webdav.login.*

import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.TodoException
import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.showToastLong
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

    private fun resourceUrl(path: String) : String {
        return "$serverUrl/$path".replace("//", "/")
    }

    fun DavResource.remoteVersion() : String {
        return "${this.etag}:${this.modified}"
    }

    override fun getRemoteVersion(filename: String): String {
        getClient()?.let {
            val url = resourceUrl(filename)
            val res = it.list(resourceUrl(filename))[0]
            Log.e(TAG, "Getting metadata for ${url}, etag: ${res.etag}, modified: ${res.modified}")
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

    override fun loadTasksFromFile(path: String): RemoteContents {

        // If we load a file and changes are pending, we do not want to overwrite
        // our local changes, instead we try to upload local
        val url = resourceUrl(path)
        Log.i(TAG, "Loading file from: $url")
        if (!isAuthenticated) {
            throw IOException("Not authenticated")
        }
        getClient()?.let {
            val version = getRemoteVersion(path)
            val readLines = it.get(url).bufferedReader(Charsets.UTF_8).readLines()
            return RemoteContents(version, readLines)
        }?:  throw TodoException("WebDav exception client is null")
    }


    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<String>, lastRemote: String? , eol: String) : String {
        getClient()?.let { client ->
            val url = resourceUrl(path)
            Log.i(TAG, "Uploading file to $url")
            val contents = join(lines, eol) + eol
            val remoteVersion = getRemoteVersion(url)
            val savedUrl = if (lastRemote == null || remoteVersion == lastRemote) {
                client.put(url, contents.toByteArray(Charsets.UTF_8))
                url
            } else {
                Log.i(TAG,"File conflict remote: $remoteVersion, last seen: $lastRemote ")
                val urlWithoutTxt = "\\.txt$".toRegex().replace(url, "")
                val newUrl = urlWithoutTxt + "_conflict_" + UUID.randomUUID() + ".txt"
                client.put(newUrl, contents.toByteArray(Charsets.UTF_8))
                newUrl
            }
            return getRemoteVersion(savedUrl)
        } ?:  throw TodoException("WebDav exception client is null")
    }

    @Throws(IOException::class)
    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        getClient()?.let { client ->
            val url = resourceUrl(path)
            Log.i(TAG, "Appending to file $url")
            val newLines = client.get(url).bufferedReader(Charsets.UTF_8).readLines().toMutableList()
            newLines.addAll(lines)
            val contents = join(newLines, eol) + eol
            client.put(url, contents.toByteArray(Charsets.UTF_8))
        } ?:  throw TodoException("WebDav exception client is null")


    }

    override fun writeFile(file: File, contents: String) {
        val url = resourceUrl(file.canonicalPath)
        getClient()?.let { client ->
            val url = resourceUrl(url)
            Log.i(TAG, "Writing file to $url")
            client.put(url, contents.toByteArray(Charsets.UTF_8))
        } ?:  throw TodoException("WebDav exception client is null")
    }

    private fun timeStamp() = (System.currentTimeMillis() / 1000).toString()

    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: (String) -> Unit) {
        val url = resourceUrl(file)
        getClient()?.let { client ->
            val url = resourceUrl(url)
            Log.i(TAG, "Writing file to $url")
            fileRead.invoke(client.get(url).bufferedReader(Charsets.UTF_8).readText())
        } ?:  throw TodoException("WebDav exception client is null")
    }


    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {
        getClient()?.let { client ->
            val url = resourceUrl(File(path).canonicalPath + "/")
            val result = ArrayList<FileEntry>()
            val resList = client.list(url)
            // Loop over the resulting files
            // Drop the first one as it is the current folder
            resList.forEach { res ->
                val filename = File(res.path).name
                result.add(FileEntry(filename, res.isDirectory))
            }
            return result
        } ?:  throw TodoException("WebDav exception client is null")
    }

    override fun getDefaultPath(): String {
        return "/todo.txt"
    }
}
