package nl.mpcjanssen.simpletask.remote


import android.content.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation
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
            val entered_url = nextcloud_server_url.text.toString()
            return if (entered_url.startsWith("http://", ignoreCase = true) ||
                    entered_url.startsWith("https://", ignoreCase = true)) {
                return entered_url

            } else {
                "https://${entered_url}"
            }
        }

    private val mApp = TodoApplication.app
    var username by Config.StringOrNullPreference(FileStore.NEXTCLOUD_USER)
    var password by Config.StringOrNullPreference(FileStore.NEXTCLOUD_PASS)
    var serverUrl by Config.StringOrNullPreference(FileStore.NEXTCLOUD_URL)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (TodoApplication.app.isAuthenticated) {
            switchToTodolist()
        }
        setTheme(Config.activeTheme)
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


    internal fun startLogin() {
        FileStoreActionQueue.add("login") {
            val client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(url), this, true, true)
            client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                    nextcloud_username.text.toString(),
                    nextcloud_password.text.toString()
            )
            val op = ReadRemoteFolderOperation(File("/").canonicalPath)
            val res: RemoteOperationResult = op.execute(client)
            Log.d(TAG, res.toString())
            Log.d(TAG, res.logMessage)
            Log.d(TAG, res.exception?.localizedMessage?:"No exception")
            Log.d(TAG, res.httpCode.toString())
            Log.d(TAG, res.data.joinToString (" "){ it.toString()})

            if (res.isSuccess ) {
                Log.d(TAG, "Logged in to Nextcloud: ${client.ownCloudVersion}")
                finishLogin()
            } else if (res.isSslRecoverableException){
                Log.d(TAG, "Invalid certificate")
                try {
                    val okListener = DialogInterface.OnClickListener { dialog, which ->
                        val ex = res.exception as CertificateCombinedException
                        val cert = ex.serverCertificate
                        NetworkUtils.addCertToKnownServersStore(cert, this);
                        showToastLong(this, "Certificate saved")
                        Log.d(TAG, "Server certificate saved");
                        finishLogin()
                    }
                    showConfirmationDialog(this, R.string.invalid_certificate_msg, okListener, R.string.invalid_certificate_title )

                } catch (e: Exception) {

                    Log.d(TAG, "Server certificate could not be saved in the known-servers trust store ", e);
                    showToastLong(this, "Failed to store certificate")
                }
            } else {
                showToastLong(this, "Login failed: ${res.code.name}")
                Log.d(TAG, "Login failed: ${res.code.name}")
            }
        }

    }

    companion object {
        internal val TAG = LoginScreen::class.java.simpleName
    }
}

