/**
 * This file is part of Simpletask.

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * Copyright (c) 2013- Mark Janssen

 * LICENSE:

 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Mark Janssen
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * *
 * @copyright 2013- Mark Janssen
 */
package nl.mpcjanssen.simpletask

import android.app.Activity
import android.content.Intent
import android.os.Bundle


class AddTaskShortcut : ThemedNoActionBarActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        val log = Logger
        log.debug(TAG, "onCreate()")
        super.onCreate(savedInstanceState)
        setupShortcut()
        finish()
    }

    private fun setupShortcut() {
        val shortcutIntent = Intent(Intent.ACTION_MAIN)
        shortcutIntent.setClassName(this, AddTask::class.java.name)

        val intent = Intent()
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                getString(R.string.shortcut_addtask_name))
        val iconResource = Intent.ShortcutIconResource.fromContext(this,
                R.drawable.ic_launcher)
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource)

        setResult(Activity.RESULT_OK, intent)
    }

    companion object {

        private val TAG = AddTaskShortcut::class.java.simpleName
    }
}
