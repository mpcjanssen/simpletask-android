package nl.mpcjanssen.simpletask.plugin.dokuwiki


import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.content.ContextCompat
import me.smichel.android.KPreferences.Preferences
import nl.mpcjanssen.simpletask.remote.IFileStorePlugin
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl

import java.io.File
import java.io.FilenameFilter
import java.net.URL
import java.util.*

class PluginService : Service() {
    internal val DOKUWIKI_USER = "dwUser"
    internal val DOKUWIKI_PASS = "dwPass"
    internal val DOKUWIKI_URL = "dwURL"
    lateinit var conf: Preferences
    val TAG = "FileStorePlugin"

    override fun onBind(intent: Intent): IBinder {
        // Return the interface
        Log.v(TAG, "Someone is binding with $intent")
        conf =    object: Preferences(MainApplication.app,"Settings") {}
        return object : IFileStorePlugin.Stub() {
            var username by conf.StringOrNullPreference(DOKUWIKI_USER)
            var password by conf.StringOrNullPreference(DOKUWIKI_PASS)
            var serverUrl by conf.StringOrNullPreference(DOKUWIKI_URL)
            val client: XmlRpcClient
                get() {

                    val config = XmlRpcClientConfigImpl()
                    config.basicUserName = username
                    config.basicPassword = password
                    config.setServerURL(URL(serverUrl + "/lib/exe/xmlrpc.php"))
                    return XmlRpcClient().also { it.setConfig(config) }
                }
            private fun wikiPath(path: String): String {
                return path.replace("/", ":")
            }

            override fun login() {
                val loginActivity = LoginScreen::class.java
                loginActivity.let {
                    val intent = Intent(this@PluginService, it)
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    this@PluginService.startActivity(intent)
                }
            }

            override fun loadTasksFromFile(path: String, lines: MutableList<String>): String {
                lines.clear()
                val content = client.execute("wiki.getPage", arrayOf(wikiPath(path))) as String
                lines.addAll(content.lines())
                return getRemoteVersion(wikiPath(path))
            }


            override fun getDefaultPath(): String {
                return "/todo/todo"
            }

            override fun isAuthenticated(): Boolean {
                return username != null
            }

            override fun getRemoteVersion(filename: String): String {
                val result = client.execute("wiki.getPageInfo", arrayOf(wikiPath(filename)))
                @Suppress("UNCHECKED_CAST")
                return (result as HashMap<String, Any>).getOrElse("version", { "" }).toString()
            }

            override fun saveTasksToFile(
                path: String,
                lines: MutableList<String>,
                eol: String,
                append: Boolean
            ): String {
                if (!append) {
                    client.execute(
                        "wiki.putPage", arrayOf(
                            wikiPath(path),
                            lines.joinToString(separator = "\n"),
                            emptyArray<String>()
                        )
                    )
                } else {
                    client.execute(
                        "dokuwiki.appendPage", arrayOf(
                            wikiPath(path),
                            "\n" + lines.joinToString(separator = "\n"),
                            emptyArray<String>()
                        )
                    )
                }
                return getRemoteVersion(wikiPath(path))
            }

            override fun readFile(path: String): String {
                val content = client.execute("wiki.getPage", arrayOf(wikiPath(path))) as String
                return content
            }

            override fun loadFileList(
                path: String,
                txtOnly: Boolean,
                folders: MutableList<String>,
                files: MutableList<String>
            ): Boolean {
                return true
            }

            override fun writeFile(path: String, contents: String) {
                client.execute(
                    "wiki.putPage", arrayOf(
                        wikiPath(path),
                        contents,
                        emptyArray<String>()
                    )
                )
            }

            override fun logout() {
                // NOOP
            }

        }
    }
}
