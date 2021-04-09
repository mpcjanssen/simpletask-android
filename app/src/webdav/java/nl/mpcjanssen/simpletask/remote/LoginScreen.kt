package nl.mpcjanssen.simpletask.remote


import android.content.*
import android.os.Bundle
import android.util.Log

import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine

import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.databinding.LoginBinding
import nl.mpcjanssen.simpletask.util.FileStoreActionQueue
import nl.mpcjanssen.simpletask.util.showToastLong
import java.lang.Exception


class LoginScreen : ThemedActionBarActivity() {
    private val url: String

        get () {
            val enteredUrl = binding.webdavServerUrl.text.toString().trimEnd('/')
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
        username = binding.webdavUsername.text.toString()
        password = binding.webdavPassword.text.toString()
        serverUrl = url
        Log.d(TAG, "Saved credentials for $username")
        switchToTodolist()
    }


    private fun startLogin() {
        FileStoreActionQueue.add("login") {
            try {
                val client = OkHttpSardine().also { it.setCredentials(binding.webdavUsername.text.toString(), binding.webdavPassword.text.toString()) }
                client.exists(binding.webdavServerUrl.text.toString() + "/")
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

