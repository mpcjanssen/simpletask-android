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
import com.dropbox.core.android.Auth
import kotlinx.android.synthetic.dropbox.login.*
import nl.mpcjanssen.simpletask.*

class LoginScreen : ThemedNoActionBarActivity() {

    private lateinit var m_app: TodoApplication
    var resumeAfterAuth = false
    private lateinit var m_broadcastReceiver: BroadcastReceiver
    private lateinit var localBroadcastManager: LocalBroadcastManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        m_app = application as TodoApplication
        setTheme(TodoApplication.config.activeTheme)
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
            TodoApplication.config.fullDropBoxAccess = true
            startLogin()
        }

        login_folder.setOnClickListener {
            TodoApplication.config.fullDropBoxAccess = false
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

    override fun onResume() {
        super.onResume()
        if (resumeAfterAuth) {
            finishLogin()
        }
    }

    private fun finishLogin() {
        val accessToken = Auth.getOAuth2Token()
        if (accessToken != null) {
            //Store accessToken in SharedPreferences
            FileStore.accessToken = accessToken
            resumeAfterAuth = false
            //Proceed to MainActivity
            TodoApplication.config.setTodoFile(FileStore.getDefaultFile())
            FileStore.remoteTodoFileChanged()
            switchToTodolist()
        }
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
        Auth.startOAuth2Authentication(this, appKey.substring(3))

    }

    companion object {

        internal val TAG = LoginScreen::class.java.simpleName
    }
}
