/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * Copyright (c) 2015 Vojtech Kral

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
 * *
 * @copyright 2015 Vojtech Kral
 */
package nl.mpcjanssen.simpletask

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.*
import android.view.MenuItem

import nl.mpcjanssen.simpletask.util.*


class Preferences : ThemedPreferenceActivity(),  SharedPreferences.OnSharedPreferenceChangeListener {

    private var prefs: SharedPreferences

    init {
        prefs = TodoApplication.prefs
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        // require restart with UI changes
        if ("theme" == key || "fontsize" == key) {
            prefs.edit().commit()
            restartToIntent(application, Intent(application, nl.mpcjanssen.simpletask.Preferences::class.java));
        }
    }

    override fun onResume() {
        super.onResume()
        // Set up a listener whenever a key changes
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        // Set up a listener whenever a key changes
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onBuildHeaders(target: List<PreferenceActivity.Header> ) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    override fun isValidFragment(fragmentName: String) : Boolean {
        return true;
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
        // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                finish()
                return true
            }
            else -> {
                return false
            }
        }
    }

    abstract class PrefFragment(val xmlId: Int) : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(xmlId)
        }

    }

    class AppearancePrefFragment : PrefFragment(R.xml.appearance_preferences)
    class InterfacePrefFragment : PrefFragment(R.xml.interface_preferences)
    class WidgetPrefFragment : PrefFragment(R.xml.widget_preferences)
    class AboutPrefFragment : PrefFragment(R.xml.about_preferences)

    companion object {
        internal val TAG = Preferences::class.java.simpleName
    }
}
