package nl.mpcjanssen.simpletask.remote



import kotlinx.android.synthetic.dokuwiki.login.*
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.Config

import nl.mpcjanssen.simpletask.util.join
import nl.mpcjanssen.simpletask.util.showToastLong
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.net.URL

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

    val client : XmlRpcClient
    get() {

        val config = XmlRpcClientConfigImpl()
        config.basicUserName = username
        config.basicPassword = password
        config.setServerURL(URL(serverUrl + "/lib/exe/xmlrpc.php"))
        return XmlRpcClient().also { it.setConfig(config) }
    }



    override val isAuthenticated: Boolean
        get() {
            return username != null
        }

    override fun logout() {
        username = null
        password = null
        serverUrl = null
    }

    private fun wikiPath(path: String): String {
        return path.replace("/",":3+")
    }



    override fun getRemoteVersion(filename: String): String {
        val result = client.execute("wiki.getPageInfo", arrayOf(wikiPath(filename)))
        return result as String
    }

    override val isOnline: Boolean
        get() {
            return true
        }

    override fun loadTasksFromFile(path: String): RemoteContents {
        val content = client.execute("wiki.getPage", arrayOf(wikiPath(path))) as String
        return RemoteContents("", content.lines())
    }


    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<String>, eol: String) {
        client.execute("wiki.putPage", arrayOf(wikiPath(path), lines.joinToString(eol), emptyArray<String>()))
    }

    @Throws(IOException::class)
    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        if (!isOnline) {
            throw IOException("Device is offline")
        }


    }

    override fun writeFile(file: File, contents: String) {


    }

    private fun timeStamp() = (System.currentTimeMillis() / 1000).toString()

    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: (String) -> Unit) {

    }


    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {
        return emptyList()
    }

    override fun getDefaultPath(): String {
        return "todo:todo.txt"
    }

}
