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
import android.support.v4.content.LocalBroadcastManager
import android.widget.Button
import com.dropbox.client2.DropboxAPI
import com.dropbox.client2.android.AndroidAuthSession
import com.dropbox.client2.session.AppKeyPair
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.util.Config


class LoginScreen : ThemedNoActionBarActivity() {

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

        var m_LoginButton = findViewById(R.id.login) as Button
        m_LoginButton.setOnClickListener {
            Config.fullDropBoxAccess = true
            startLogin()
        }

        m_LoginButton = findViewById(R.id.login_folder) as Button
        m_LoginButton.setOnClickListener {
            Config.fullDropBoxAccess = false
            startLogin()
        }


        if (m_app.isAuthenticated) {
            switchToTodolist()
        }

    }
    fun getMdbApi(mApp: TodoApplication) : DropboxAPI<AndroidAuthSession> {
        val app_secret: String
        var app_key: String

            // Full access or folder access?
            if (Config.fullDropBoxAccess) {
                app_secret = mApp.getString(R.string.dropbox_consumer_secret)
                app_key = mApp.getString(R.string.dropbox_consumer_key)
            } else {
                app_secret = mApp.getString(R.string.dropbox_folder_consumer_secret)
                app_key = mApp.getString(R.string.dropbox_folder_consumer_key)
            }
            app_key = app_key.replaceFirst("^db-".toRegex(), "")
            // And later in some initialization function:
            val appKeys = AppKeyPair(app_key, app_secret)
            val savedAuth : String? = null
            val session = AndroidAuthSession(appKeys, savedAuth)
            return DropboxAPI(session)

    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        finishLogin()
    }

    private fun finishLogin() {
        if (m_app.isAuthenticated) {
            m_app.fileChanged(Config.todoFileName)
            switchToTodolist()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(m_broadcastReceiver)
    }

    internal fun startLogin() {
        getMdbApi(m_app).session.startOAuth2Authentication(this)
    }


    companion object {

        internal val TAG = LoginScreen::class.java.simpleName
    }


}
