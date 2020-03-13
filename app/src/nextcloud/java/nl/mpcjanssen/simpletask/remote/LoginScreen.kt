package nl.mpcjanssen.simpletask.remote


import android.content.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation

import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation

import kotlinx.android.synthetic.nextcloud.login.*
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.FileStoreActionQueue
import nl.mpcjanssen.simpletask.util.showConfirmationDialog
import nl.mpcjanssen.simpletask.util.showToastLong
import java.io.File


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
        username = nextcloud_username.text.toString()
        password = nextcloud_password.text.toString()
        serverUrl = url
        Log.d(TAG, "Saved credentials for $username")
        switchToTodolist()
    }


    private fun startLogin(retrySsl: Boolean) {
        FileStoreActionQueue.add("login") {
            OwnCloudClientManagerFactory.setUserAgent("Mozilla/5.0 (Android) ownCloud-android/3.8.1")
            val client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(url), this, true)
            client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                    nextcloud_username.text.toString(),
                    nextcloud_password.text.toString()
            )
            val op = GetUserInfoRemoteOperation()
            val res: RemoteOperationResult = op.execute(client)
            Log.d(TAG, res.logMessage)
            Log.d(TAG, res.exception?.localizedMessage?:"No exception")
            Log.d(TAG, res.httpCode.toString())
            res.data?.let {
                Log.d(TAG, it.joinToString(" "))
            }

            when {
                res.isSuccess -> {
                    Log.d(TAG, "Logged in to Nextcloud: ${client.ownCloudVersion}")
                    finishLogin()
                }
                res.isSslRecoverableException && retrySsl -> {
                    Log.d(TAG, "Invalid certificate")
                    runOnUiThread {
                        try {
                            val okListener = DialogInterface.OnClickListener { _, _ ->
                                val ex = res.exception as CertificateCombinedException
                                val cert = ex.serverCertificate
                                NetworkUtils.addCertToKnownServersStore(cert, this)
                                showToastLong(this, "Certificate saved")
                                Log.d(TAG, "Server certificate saved")
                                startLogin(false)
                            }
                            showConfirmationDialog(this, R.string.invalid_certificate_msg, okListener, R.string.invalid_certificate_title)

                        } catch (e: Exception) {

                            Log.d(TAG, "Server certificate could not be saved in the known-servers trust store ", e)
                            showToastLong(this, "Failed to store certificate")
                        }
                    }
                }
                else -> {
                    showToastLong(this, "Login failed: ${res.code.name}")
                    Log.d(TAG, "Login failed: ${res.code.name}")
                }
            }
        }

    }

    companion object {
        internal val TAG = LoginScreen::class.java.simpleName
    }
}

