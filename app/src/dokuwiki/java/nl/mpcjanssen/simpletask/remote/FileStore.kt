package nl.mpcjanssen.simpletask.remote



import android.util.Log
import nl.mpcjanssen.simpletask.TodoApplication
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.reflect.KClass

/**
 * FileStore implementation backed by a Dokuwiki page
 */
object FileStore : IFileStore {
    const val TAG = "DokuwikiStore"
    internal const val DOKUWIKI_USER = "dwUser"
    internal const val DOKUWIKI_PASS = "dwPass"
    internal const val DOKUWIKI_URL = "dwURL"

    var username by TodoApplication.config.StringOrNullPreference(DOKUWIKI_USER)
    var password by TodoApplication.config.StringOrNullPreference(DOKUWIKI_PASS)
    private var serverUrl by TodoApplication.config.StringOrNullPreference(DOKUWIKI_URL)

    private val client : XmlRpcClient
    get() {

        val config = XmlRpcClientConfigImpl()
        config.basicUserName = username
        config.basicPassword = password
        config.serverURL = URL("$serverUrl/lib/exe/xmlrpc.php")
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
        TodoApplication.config.setTodoFile(getDefaultPath())
        TodoApplication.config.clearCache()
    }

    private fun wikiPath(path: String): String {
        return path.replace("/",":")
    }

    override fun getRemoteVersion(filename: String): String {
        val result = client.execute("wiki.getPageInfo", arrayOf(wikiPath(filename)))
        @Suppress("UNCHECKED_CAST")
        return (result as HashMap<String,Any>).getOrElse("version", {""}).toString()
    }

    override val isOnline: Boolean
        get() {
            return true
        }

    override fun loadTasksFromFile(path: String): RemoteContents {
        val content = client.execute("wiki.getPage", arrayOf(wikiPath(path))) as String
        return RemoteContents(getRemoteVersion(wikiPath(path)), content.lines())
    }

    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<String>, lastRemote: String?,  eol: String) : String {
        client.execute("wiki.putPage", arrayOf(wikiPath(path),
                lines.joinToString(separator = "\n"),
                emptyArray<String>()))
        return getRemoteVersion(wikiPath(path))
    }

    @Throws(IOException::class)
    override fun appendTaskToFile(path: String, lines: List<String>, eol: String) {
        client.execute("dokuwiki.appendPage", arrayOf(wikiPath(path),
                "\n"+lines.joinToString(separator = "\n"),
                emptyArray<String>()))
    }

    override fun writeFile(path: String, contents: String) {
        client.execute("wiki.putPage", arrayOf(
                wikiPath(path),
                contents,
                emptyArray<String>()))
    }


    @Throws(IOException::class)
    override fun readFile(file: String, fileRead: (String) -> Unit) {
        val content = client.execute("wiki.getPage", arrayOf(wikiPath(file))) as String
        fileRead.invoke(content)
    }


    override fun loadFileList(path: String, txtOnly: Boolean): List<FileEntry> {

        val parent = wikiPath(File(path).parent)
        Log.i(TAG,"Getting pages in namespace $parent")
        val pages = client.execute("wiki.getAllPages", arrayOf(parent, emptyArray<String>())) as List<String>
        return pages.map {
            FileEntry(it, false)
        }
    }

    override fun getDefaultPath(): String {
        return "/todo/todo"
    }

}
