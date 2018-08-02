package nl.mpcjanssen.simpletask.remote

import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.accounts.AccountManager
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.annotation.StringRes
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.network.CertificateCombinedException
import com.owncloud.android.lib.common.network.NetworkUtils
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ReadRemoteFolderOperation
import com.owncloud.android.lib.resources.files.RemoveRemoteFileOperation
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation
import kotlinx.android.synthetic.nextcloud.login.*
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.FileStoreActionQueue
import nl.mpcjanssen.simpletask.util.showConfirmationDialog
import nl.mpcjanssen.simpletask.util.showToastLong
import java.io.File


class LoginScreen : AccountAuthenticatorActivity() {
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



    private lateinit var m_app: TodoApplication
    private lateinit var m_broadcastReceiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        m_app = application as TodoApplication
        setTheme(Config.activeTheme)
        setContentView(R.layout.login)
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        val intentFilter = IntentFilter()
        intentFilter.addAction("nl.mpcjanssen.simpletask.ACTION_LOGIN")
        m_broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val i = Intent(context, Simpletask::class.java)
                startActivity(i)
                finish()
            }
        }
        localBroadcastManager.registerReceiver(m_broadcastReceiver, intentFilter)

        login.setOnClickListener {
            startLogin()
        }

        logging.setOnClickListener {
            startActivity(Intent(this, DebugInfoScreen::class.java))
        }

        if (m_app.isAuthenticated) {
            switchToTodolist()
        }

    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
    }

    private fun finishLogin() {
        val am = AccountManager.get(this)
        val bundle = Bundle()
        bundle.putString("server_url", url)
        am.addAccountExplicitly(
                Account(nextcloud_username.text.toString(), m_app.getString(R.string.account_type)),
                nextcloud_password.text.toString(),
                bundle
        )
        switchToTodolist()
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(m_broadcastReceiver)
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
            if (res.isSuccess ) {
                Logger.debug(TAG, "Logged in to Nextcloud: ${client.ownCloudVersion}")
                finishLogin()
            } else if (res.isSslRecoverableException){
                Logger.debug(TAG, "Invalid certificate")
                try {
                    val okListener = DialogInterface.OnClickListener { dialog, which ->
                        val ex = res.exception as CertificateCombinedException
                        val cert = ex.serverCertificate
                        NetworkUtils.addCertToKnownServersStore(cert, this);
                        showToastLong(this, "Certificate saved")
                        Logger.debug(TAG, "Server certificate saved");
                        finishLogin()
                    }
                    showConfirmationDialog(this, R.string.invalid_certificate_msg, okListener, R.string.invalid_certificate_title )

                } catch (e: Exception) {

                    Logger.debug(TAG, "Server certificate could not be saved in the known-servers trust store ", e);
                    showToastLong(this, "Failed to store certificate")
                }
            } else {
                showToastLong(this, "Login failed: ${res.code.name}")
                Logger.debug(TAG, "Login failed: ${res.code.name}")
            }
        }

    }




    companion object {
        internal val TAG = LoginScreen::class.java.simpleName
    }
}
