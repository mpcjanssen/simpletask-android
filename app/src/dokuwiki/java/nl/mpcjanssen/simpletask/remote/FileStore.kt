package nl.mpcjanssen.simpletask.remote



import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.task.Task

import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import java.io.File
import java.io.IOException
import java.net.URL

import kotlin.reflect.KClass

internal val RE_TODO = Regex("<todo([^>]*)>([^<]*)</todo>")

private val s1 = System.currentTimeMillis().toString()

/**
 * FileStore implementation backed by a Dokuwiki page
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
        TodoApplication.config.setTodoFile(getDefaultPath())
        TodoApplication.config.clearCache()
    }

    private fun wikiPath(path: String): String {
        return path.replace("/",":")
    }

    override fun getRemoteVersion(filename: String): String {
        val result = client.execute("wiki.getPageInfo", arrayOf(wikiPath(filename)))
        return (result as HashMap<String,Any>).getOrElse("version", {""}).toString()
    }

    override val isOnline: Boolean
        get() {
            return true
        }

    override fun loadTasksFromFile(path: String): RemoteContents {
        val content = client.execute("wiki.getPage", arrayOf(wikiPath(path))) as String
        val tasks = RE_TODO.findAll(content).map {
            fromDokuwiki(it.value)
        }
        return RemoteContents(getRemoteVersion(wikiPath(path)), tasks.toList())
    }


    override fun loginActivity(): KClass<*>? {
        return LoginScreen::class
    }

    @Synchronized
    @Throws(IOException::class)
    override fun saveTasksToFile(path: String, lines: List<Task>, eol: String) {
        client.execute("wiki.putPage", arrayOf(wikiPath(path),
                lines.joinToString(separator = "\n") {"  * ${it.asDokuwiki()}"},
                emptyArray<String>()))
    }

    @Throws(IOException::class)
    override fun appendTaskToFile(path: String, lines: List<Task>, eol: String) {
        if (!isOnline) {
            throw IOException("Device is offline")
        }
        client.execute("dokuwiki.appendPage", arrayOf(wikiPath(path),
                "\n"+lines.joinToString(separator = "\n") {"  * <todo>${it.inFileFormat(false)}</todo>"},
                emptyArray<String>()))


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
        return "/todo/todo"
    }

}

fun Task.asDokuwiki() : String {
    return "<todo " + if (this.isCompleted()) "#$completionDate" else "" + ">" +
    this.inFileFormat(false) + "</todo>"
}

fun fromDokuwiki(content: String) : Task {
    val matches = RE_TODO.matchEntire(content)
    val t = Task(matches?.groupValues?.getOrElse(2, {""})?:"")
    return t
}