package nl.mpcjanssen.simpletask.remote

import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation
import kotlinx.android.synthetic.nextcloud.login.*
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.Config


class LoginScreen : AccountAuthenticatorActivity(), OnRemoteOperationListener {
    private val url: String
        get () {
            return "https://${nextcloud_server_url.text}"
        }

    override fun onRemoteOperationFinish(p0: RemoteOperation?, p1: RemoteOperationResult?) {
        if (p0 is GetRemoteUserInfoOperation) {
            if (p1?.httpCode == 200) {
                finishLogin()
            } else {
                Logger.error(TAG, "Exception: ${p1?.exception?.message}")
                Logger.error(TAG, "Code: ${p1?.httpCode}")
                Logger.error(TAG, "Message: ${p1?.logMessage}")
                Toast.makeText(this, getString(R.string.login_unsuccesfull), Toast.LENGTH_SHORT).show()
            }
        } else {
            Logger.error(TAG, "Exception: ${p1?.exception?.message}")
            Logger.error(TAG, "Code: ${p1?.httpCode}")
            Logger.error(TAG, "Message: ${p1?.logMessage}")
            Toast.makeText(this, getString(R.string.unexpected_result), Toast.LENGTH_SHORT).show()
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
        val isSamlAuth = AUTH_ON.equals(getString(R.string.auth_method_saml_web_sso))

        OwnCloudClientManagerFactory.setUserAgent(getUserAgent())
        OwnCloudClientManagerFactory.setNextcloudUserAgent(getNextcloudUserAgent())
        if (isSamlAuth) {
            OwnCloudClientManagerFactory.setDefaultPolicy(OwnCloudClientManagerFactory.Policy.SINGLE_SESSION_PER_ACCOUNT)
        } else {
            OwnCloudClientManagerFactory
                    .setDefaultPolicy(OwnCloudClientManagerFactory.Policy.SINGLE_SESSION_PER_ACCOUNT_IF_SERVER_SUPPORTS_SERVER_MONITORING)
        }
        val client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(url), this, true)
        client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                nextcloud_username.text.toString(),
                nextcloud_password.text.toString()
        )
        val versionOperation = GetRemoteUserInfoOperation()
        versionOperation.execute(client, this, Handler())
    }

    fun getUserAgent(): String {
        // Mozilla/5.0 (Android) ownCloud-android/1.7.0
        return getUserAgent(R.string.user_agent)
    }

    fun getNextcloudUserAgent(): String {
        // Mozilla/5.0 (Android) Nextcloud-android/2.1.0
        return getUserAgent(R.string.nextcloud_user_agent)
    }

    private fun getUserAgent(@StringRes agent: Int): String {
        val appString = applicationContext.getResources().getString(agent)
        val packageName = applicationContext.getPackageName()
        var version = ""

        try {
            val pInfo = applicationContext.getPackageManager().getPackageInfo(packageName, 0)
            if (pInfo != null) {
                version = pInfo.versionName
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Logger.error(TAG, "Trying to get packageName")
            e.cause?.let { Logger.error(TAG, "Cause:", it) }
        }

        return String.format(appString, version)
    }

    companion object {
        internal val TAG = LoginScreen::class.java.simpleName
        val AUTH_ON = "on"
    }
}
