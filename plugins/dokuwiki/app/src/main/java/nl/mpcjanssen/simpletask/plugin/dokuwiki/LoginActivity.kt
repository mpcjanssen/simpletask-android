package nl.mpcjanssen.simpletask.plugin.dokuwiki


import android.app.Activity
import android.content.*
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.login.*
import me.smichel.android.KPreferences.Preferences


import org.apache.xmlrpc.client.XmlRpcClient
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast

import java.net.URL



class LoginScreen : Activity () {
    internal val DOKUWIKI_USER = "dwUser"
    internal val DOKUWIKI_PASS = "dwPass"
    internal val DOKUWIKI_URL = "dwURL"
    val conf  = object: Preferences(MainApplication.app,"Settings") {}
    private val url: String
        get () {
            val enteredUrl = server_url.text.toString().trimEnd('/')
            return if (enteredUrl.startsWith("http://", ignoreCase = true) ||
                enteredUrl.startsWith("https://", ignoreCase = true)) {
                return enteredUrl
            } else {
                "https://$enteredUrl"
            }
        }

    private var usernamePref by conf.StringOrNullPreference(DOKUWIKI_USER)
    private var passwordPref by conf.StringOrNullPreference(DOKUWIKI_PASS)
    private var serverUrlPref by conf.StringOrNullPreference(DOKUWIKI_URL)




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login)

        login.setOnClickListener {
            startLogin()
        }

    }

    private fun switchToTodolist() {
        finish()
    }

    private fun finishLogin() {
        usernamePref = username.text.toString()
        passwordPref = password.text.toString()
        serverUrlPref = url
        Log.d(TAG, "Saved credentials for $username")
        switchToTodolist()
    }


    private fun startLogin() {

            try {
                val config = XmlRpcClientConfigImpl()
                config.basicUserName = username.text.toString()
                config.basicPassword = password.text.toString()
                config.setServerURL(URL(url + "/lib/exe/xmlrpc.php"))
                val client = XmlRpcClient()
                client.setConfig(config)

                val version = client.execute("dokuwiki.getVersion", emptyList<String>()) as String

                Log.d(TAG, "Logged in to dokuwiki: $version")
                finishLogin()

            } catch (e: Exception) {
                longToast("Login failed: ${e.message}")
                Log.d(TAG, "Login failed:", e)
            }

    }

    companion object {
        internal val TAG = LoginScreen::class.java.simpleName
    }
}
