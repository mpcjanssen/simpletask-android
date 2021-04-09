package nl.mpcjanssen.simpletask.remote


import android.content.*
import android.os.Bundle
import android.util.Log

import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.databinding.LoginBinding
import nl.mpcjanssen.simpletask.util.FileStoreActionQueue
import nl.mpcjanssen.simpletask.util.showToastLong
import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl

import java.net.URL


class LoginScreen : ThemedActionBarActivity() {
    private val url: String
        get () {
            val enteredUrl = binding.serverUrl.text.toString().trimEnd('/')
            return if (enteredUrl.startsWith("http://", ignoreCase = true) ||
                    enteredUrl.startsWith("https://", ignoreCase = true)) {
                return enteredUrl
            } else {
                "https://$enteredUrl"
            }
        }

    private var usernamePref by TodoApplication.config.StringOrNullPreference(FileStore.DOKUWIKI_USER)
    private var passwordPref by TodoApplication.config.StringOrNullPreference(FileStore.DOKUWIKI_PASS)
    private var serverUrlPref by TodoApplication.config.StringOrNullPreference(FileStore.DOKUWIKI_URL)
    private lateinit var binding: LoginBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (TodoApplication.app.isAuthenticated) {
            switchToTodolist()
        }
        setTheme(TodoApplication.config.activeTheme)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.login.setOnClickListener {
            startLogin()
        }

        binding.logging.setOnClickListener {
            startActivity(Intent(this, DebugInfoScreen::class.java))
        }
    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
    }

    private fun finishLogin() {
        usernamePref = binding.username.text.toString()
        passwordPref = binding.password.text.toString()
        serverUrlPref = url
        Log.d(TAG, "Saved credentials for ${binding.username}")
        switchToTodolist()
    }


    private fun startLogin() {
        FileStoreActionQueue.add("login") {
            try {
                val config = XmlRpcClientConfigImpl()
                config.basicUserName = binding.username.text.toString()
                config.basicPassword = binding.password.text.toString()
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

