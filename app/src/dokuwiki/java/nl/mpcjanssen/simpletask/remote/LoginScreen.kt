package nl.mpcjanssen.simpletask.remote


import android.content.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.dokuwiki.login.*

import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.FileStoreActionQueue
import nl.mpcjanssen.simpletask.util.showConfirmationDialog
import nl.mpcjanssen.simpletask.util.showToastLong
import java.io.File
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl

import java.net.URL


class LoginScreen : ThemedActionBarActivity() {
    private val url: String
        get () {
            val enteredUrl = nextcloud_server_url.text.toString().trimEnd('/')
            return if (enteredUrl.startsWith("http://", ignoreCase = true) ||
                    enteredUrl.startsWith("https://", ignoreCase = true)) {
                return enteredUrl
            } else {
                "https://$enteredUrl"
            }
        }

    private var username by TodoApplication.config.StringOrNullPreference(FileStore.NEXTCLOUD_USER)
    private var password by TodoApplication.config.StringOrNullPreference(FileStore.NEXTCLOUD_PASS)
    private var serverUrl by TodoApplication.config.StringOrNullPreference(FileStore.NEXTCLOUD_URL)




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (TodoApplication.app.isAuthenticated) {
            switchToTodolist()
        }
        setTheme(TodoApplication.config.activeTheme)
        setContentView(R.layout.login)

        login.setOnClickListener {
            startLogin()
        }

        logging.setOnClickListener {
            startActivity(Intent(this, DebugInfoScreen::class.java))
        }
    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
    }

    private fun finishLogin() {
        username = nextcloud_username.text.toString()
        password = nextcloud_password.text.toString()
        serverUrl = url
        Log.d(TAG, "Saved credentials for $username")
        switchToTodolist()
    }


    private fun startLogin() {
        FileStoreActionQueue.add("login") {
            try {
                val config = XmlRpcClientConfigImpl()
                config.basicUserName = nextcloud_username.text.toString()
                config.basicPassword = nextcloud_password.text.toString()
                config.setServerURL(URL(url + "/lib/exe/xmlrpc.php"))
                val client = XmlRpcClient()
                client.setConfig(config)

                val version = client.execute("dokuwiki.getVersion", emptyList<String>()) as String

                Log.d(TAG, "Logged in to dokuwiki: $version")
                finishLogin()

            } catch (e: Exception) {
                showToastLong(this, "Login failed: ${e.message}")
                Log.d(TAG, "Login failed:", e)
            }
        }

    }

    companion object {
        internal val TAG = LoginScreen::class.java.simpleName
    }
}

