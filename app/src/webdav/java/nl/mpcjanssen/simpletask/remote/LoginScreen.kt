package nl.mpcjanssen.simpletask.remote


import android.content.*
import android.net.Uri
import android.os.Bundle
import android.util.Log

import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine

import kotlinx.android.synthetic.webdav.login.*
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.FileStoreActionQueue
import nl.mpcjanssen.simpletask.util.showConfirmationDialog
import nl.mpcjanssen.simpletask.util.showToastLong
import java.lang.Exception


class LoginScreen : ThemedActionBarActivity() {
    private val url: String
        get () {
            val enteredUrl = webdav_server_url.text.toString().trimEnd('/')
            return if (enteredUrl.startsWith("http://", ignoreCase = true) ||
                    enteredUrl.startsWith("https://", ignoreCase = true)) {
                 enteredUrl
            } else {
                "https://$enteredUrl"
            }.trimEnd('/')
        }

    private var username by TodoApplication.config.StringOrNullPreference(FileStore.USER)
    private var password by TodoApplication.config.StringOrNullPreference(FileStore.PASS)
    private var serverUrl by TodoApplication.config.StringOrNullPreference(FileStore.URL)




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (TodoApplication.app.isAuthenticated) {
            switchToTodolist()
        }
        setTheme(TodoApplication.config.activeTheme)
        setContentView(R.layout.login)

        login.setOnClickListener {
            startLogin(true)
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
        username = webdav_username.text.toString()
        password = webdav_password.text.toString()
        serverUrl = url
        TodoApplication.config.setTodoFile("$url/${FileStore.getDefaultPath()}")
        Log.d(TAG, "Saved credentials for $username")
        switchToTodolist()
    }


    private fun startLogin(retrySsl: Boolean) {
        FileStoreActionQueue.add("login") {
            try {
                val client = OkHttpSardine().also { it.setCredentials(webdav_username.text.toString(), webdav_password.text.toString()) }
                client.exists(webdav_server_url.text.toString() + "/")
                finishLogin()
            } catch (e: Exception) {
                Log.i(TAG, "Login failed", e)
                showToastLong(this, "Login failed: ${e.message}")
            }
        }

    }

    companion object {
        internal val TAG = LoginScreen::class.java.simpleName
    }
}

