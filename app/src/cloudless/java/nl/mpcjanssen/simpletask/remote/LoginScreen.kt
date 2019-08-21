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

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.cloudless.login.*
import nl.mpcjanssen.simpletask.R
import nl.mpcjanssen.simpletask.Simpletask
import nl.mpcjanssen.simpletask.ThemedNoActionBarActivity
import nl.mpcjanssen.simpletask.TodoApplication
import nl.mpcjanssen.simpletask.util.Config
import nl.mpcjanssen.simpletask.util.showToastLong

class LoginScreen : ThemedNoActionBarActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FileStore.isAuthenticated) {
            switchToTodolist()
        }
        setTheme(TodoApplication.config.activeTheme)
        setContentView(R.layout.login)

        val loginButton = login
        loginButton.setOnClickListener {
            startLogin()
        }
    }

    private fun switchToTodolist() {
        val intent = Intent(this, Simpletask::class.java)
        startActivity(intent)
        finish()
    }

    private fun finishLogin() {

        if (FileStore.isAuthenticated) {
            switchToTodolist()
        } else {
            showToastLong(this, "Storage access denied")
        }
    }

    internal fun startLogin() {
        ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION -> finishLogin()
        }
    }

    companion object {
        private val REQUEST_PERMISSION = 1
        internal val TAG = LoginScreen::class.java.simpleName
    }
}
