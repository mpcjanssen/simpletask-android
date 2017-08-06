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

import android.accounts.Account
import android.accounts.AccountAuthenticatorActivity
import android.accounts.AccountManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentialsFactory
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.Simpletask
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.Config

class LoginScreen : AccountAuthenticatorActivity(), OnRemoteOperationListener {
    override fun onRemoteOperationFinish(p0: RemoteOperation?, p1: RemoteOperationResult?) {
        if (p0 is GetRemoteUserInfoOperation) {
            if (p1!!.httpCode == 200) {
                finishLogin()
            } else {
                Toast.makeText(this, getString(R.string.login_unsuccesfull), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, getString(R.string.unexpected_result), Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var m_app: TodoApplication
    private lateinit var m_broadcastReceiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var m_server: EditText
    private lateinit var m_username: EditText
    private lateinit var m_password: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        m_app = application as TodoApplication
        setTheme(Config.activeTheme)
        setContentView(R.layout.login_nextcloud)
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

        var loginButton = findViewById(R.id.login) as Button
        loginButton.setOnClickListener {
            startLogin()
        }

        m_server = findViewById(R.id.nextcloud_server_url) as EditText
        m_username = findViewById(R.id.nextcloud_username) as EditText
        m_password = findViewById(R.id.nextcloud_password) as EditText

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
    }

    private fun finishLogin() {
        val am = AccountManager.get(this)
        val bundle = Bundle()
        bundle.putString("server_url", m_server.text.toString())
        am.addAccountExplicitly(
                Account(m_username.text.toString(), m_app.getString(R.string.account_type)),
                m_password.text.toString(),
                bundle
        )
        switchToTodolist()
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(m_broadcastReceiver)
    }

    internal fun startLogin() {
        val client = OwnCloudClientFactory.createOwnCloudClient(Uri.parse(m_server.text.toString()), this, true)
        client.credentials = OwnCloudCredentialsFactory.newBasicCredentials(
                m_username.text.toString(),
                m_password.text.toString()
        )
        val versionOperation = GetRemoteUserInfoOperation()
        versionOperation.execute(client, this, Handler())
    }

    companion object {
        internal val TAG = LoginScreen::class.java.simpleName
    }
}
