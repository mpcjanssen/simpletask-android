package nl.mpcjanssen.simpletask.remote



import nl.mpcjanssen.simpletask.TodoApplication

import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.io.File
import java.io.IOException
import java.net.URL

import kotlin.reflect.KClass

private fun File.wikiPath(): String {
    return canonicalPath.replace("/",":")
}

/**
 * FileStore implementation backed by a Dokuwiki page
 */
object FileStore : IFileStore {

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
        TodoApplication.config.setTodoFile(getDefaultFile())
        TodoApplication.config.clearCache()
    }



    override fun getRemoteVersion(file: File): String {
        val result = client.execute("wiki.getPageInfo", arrayOf(file.wikiPath()))
        @Suppress("UNCHECKED_CAST")
        return (result as HashMap<String,Any>).getOrElse("version", {""}).toString()
    }

    override val isOnline: Boolean
        get() {
            return true
        }

    override fun loadTasksFromFile(file: File): RemoteContents {
        val content = client.execute("wiki.getPage", arrayOf(file.wikiPath())) as String
        return RemoteContents(getRemoteVersion(file), content.lines())
    }

    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(file: File, lines: List<String>, lastRemote: String?, eol: String) : String {
        client.execute("wiki.putPage", arrayOf(
                file.wikiPath(),
                lines.joinToString(separator = "\n"),
                emptyArray<String>()))
        return getRemoteVersion(file)
    }

    @Throws(IOException::class)
    override fun appendTaskToFile(file: File, lines: List<String>, eol: String) {
        client.execute("dokuwiki.appendPage", arrayOf(
                file.wikiPath(),
                "\n"+lines.joinToString(separator = "\n"),
                emptyArray<String>()))
    }

    override fun writeFile(file: File, contents: String) {
        client.execute("wiki.putPage", arrayOf(
                file.wikiPath(),
                contents,
                emptyArray<String>()))
    }


    @Throws(IOException::class)
    override fun readFile(file: File, fileRead: (String) -> Unit) {
        val content = client.execute("wiki.getPage", arrayOf(file.wikiPath())) as String
        fileRead.invoke(content)
    }


    override fun loadFileList(file: File, txtOnly: Boolean): List<FileEntry> {
        return emptyList()
    }

    override fun getDefaultFile(): File {
        return File("/todo/todo")
    }

}
