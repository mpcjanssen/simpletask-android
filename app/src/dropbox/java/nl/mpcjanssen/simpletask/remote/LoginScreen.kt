/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)

 * LICENSE:

 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.simpletask.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle

import android.widget.Button
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.databinding.LoginBinding

class LoginScreen : ThemedNoActionBarActivity() {

    private lateinit var m_app: TodoApplication
    var resumeAfterAuth = false
    private lateinit var m_broadcastReceiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var binding: LoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        m_app = application as TodoApplication
        setTheme(TodoApplication.config.activeTheme)
        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
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

        binding.login.setOnClickListener {
            TodoApplication.config.fullDropBoxAccess = true
            startLogin()
        }

        binding.loginFolder.setOnClickListener {
            TodoApplication.config.fullDropBoxAccess = false
            startLogin()
        }

        binding.logging.setOnClickListener {
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

    override fun onResume() {
        super.onResume()
        if (resumeAfterAuth) {
            finishLogin()
        }
    }

    private fun finishLogin() {
        val credential = Auth.getDbxCredential() //fetch the result from the AuthActivity
        credential?.let {
            //the user successfully connected their Dropbox account!
            storeCredentialLocally(it)

            resumeAfterAuth = false
            //Proceed to MainActivity
            TodoApplication.config.setTodoFile(FileStore.getDefaultFile())
            FileStore.remoteTodoFileChanged()
            switchToTodolist()
        }

    }

    //serialize the credential and store in SharedPreferences
    private fun storeCredentialLocally(dbxCredential: DbxCredential) {
        val sharedPreferences = getSharedPreferences("dropbox", MODE_PRIVATE)
        sharedPreferences.edit().putString("credential", dbxCredential.toString()).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(m_broadcastReceiver)
        }

    internal fun startLogin() {
        TodoApplication.config.clearCache()
        val appKey =
                if (TodoApplication.config.fullDropBoxAccess) {
                    m_app.getString(R.string.dropbox_consumer_key)
                } else {
                    m_app.getString(R.string.dropbox_folder_consumer_key)
                }
        resumeAfterAuth = true
        // The client identifier is usually of the form "SoftwareName/SoftwareVersion".
        val requestConfig = DbxRequestConfig(FileStore.clientIdentifier())

        // The scope's your app will need from Dropbox
        // Read more about Scopes here: https://developers.dropbox.com/oauth-guide#dropbox-api-permissions
        val scopes = listOf("account_info.read", "files.content.write", "files.content.read")
        Auth.startOAuth2PKCE(this, appKey.substring(3), requestConfig, scopes)

    }

    companion object {

        internal val TAG = LoginScreen::class.java.simpleName
    }
}
