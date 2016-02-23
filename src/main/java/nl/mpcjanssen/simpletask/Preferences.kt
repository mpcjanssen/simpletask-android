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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceCategory
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import nl.mpcjanssen.simpletask.util.*


class Preferences : ThemedActivity() {
    private var localBroadcastManager: LocalBroadcastManager? = null

    private fun broadcastIntentAndClose(intent: String, result: Int) {

        val broadcastIntent = Intent(intent)
        localBroadcastManager!!.sendBroadcast(broadcastIntent)

        // Close preferences screen
        setResult(result)
        finish()
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Display the fragment as the main content.
        val prefFragment = TodoTxtPrefFragment()
        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        fragmentManager.beginTransaction().replace(android.R.id.content, prefFragment).commit()
    }


    class TodoTxtPrefFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {
        private lateinit var m_app: TodoApplication
        private val log = Logger

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
            val packageInfo: PackageInfo
            val versionPref = findPreference("app_version")
            try {
                packageInfo = activity.packageManager.getPackageInfo(
                        activity.packageName, 0)
                versionPref.title = "Simpletask " + BuildConfig.FLAVOR + " v" + packageInfo.versionName + " (" + BuildConfig.VERSION_CODE + ")"
            } catch (e: NameNotFoundException) {
                e.printStackTrace()
            }

            m_app = activity.application as TodoApplication
            if (m_app.storeType() != Constants.STORE_DROPBOX) {
                val dropboxCategory = findPreference(getString(R.string.dropbox_cat_key)) as PreferenceCategory
                preferenceScreen.removePreference(dropboxCategory)
            }
            val toHide: Preference
            val aboutCategory = findPreference("about") as PreferenceCategory
            if (m_app.hasDonated()) {
                toHide = findPreference("donate")
            } else {
                toHide = findPreference("donated")
            }
            aboutCategory.removePreference(toHide)

            if (!TodoApplication.atLeastAPI(16)) {
                val calSyncPref = findPreference(getString(R.string.calendar_sync_screen))
                val behaviorCategory = findPreference(getString(R.string.experimental_cat_key)) as PreferenceCategory
                behaviorCategory.removePreference(calSyncPref)
            }
        }

        override fun onResume() {
            super.onResume()
            // Set up a listener whenever a key changes
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            // Set up a listener whenever a key changes
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
            // require restart with UI changes
            if ("theme" == key || "fontsize" == key) {
                activity.setResult(RESULT_RECREATE_ACTIVITY)
                activity.finish()
            }
            if (key.equals(getString(R.string.calendar_sync_dues)) ||
                key.equals(getString(R.string.calendar_sync_thresholds))) {
                if (m_app.isSyncDues || m_app.isSyncThresholds) {
                    /* Check for calendar permission */
                    val permissionCheck = ContextCompat.checkSelfPermission(m_app,
                            Manifest.permission.WRITE_CALENDAR)

                    if (permissionCheck == PackageManager.PERMISSION_DENIED) {
                        ActivityCompat.requestPermissions(activity,
                                arrayOf(Manifest.permission.WRITE_CALENDAR), 0);
                    }
                }
            }
        }

        override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen,
                                           preference: Preference): Boolean {

            val key = preference.key ?: return false
            when (key) {
                "archive_now" -> {
                    log.info(TAG, "Archiving completed items from preferences")
                    m_app.showConfirmationDialog(activity, R.string.delete_task_message,
                        DialogInterface.OnClickListener() {dialogInterface, i -> (activity as Preferences).broadcastIntentAndClose(
                                Constants.BROADCAST_ACTION_ARCHIVE,
                                Preferences.RESULT_ARCHIVE)
                    }, R.string.archive_task_title)
                }
                "logout_dropbox" -> {
                    log.info(TAG, "Logging out from Dropbox")
                    m_app.showConfirmationDialog(activity, R.string.logout_message,
                            DialogInterface.OnClickListener() { dialogInterface, i ->
                        (activity as Preferences).broadcastIntentAndClose(
                                Constants.BROADCAST_ACTION_LOGOUT,
                                RESULT_LOGOUT)
                    }, R.string.dropbox_logout_pref_title)
                }
                "send_log" -> startActivity(Intent(activity, LogScreen::class.java))
                "share_history" -> startActivity(Intent(this.activity, HistoryScreen::class.java))
                "share_modification" -> startActivity(Intent(this.activity, ModificationScreen::class.java))
                "app_version" -> {
                    val clipboard = m_app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val versionPref = findPreference("app_version")
                    val clip = ClipData.newPlainText(getString(R.string.version_copied), versionPref.title)
                    clipboard.primaryClip = clip
                    showToastShort(activity, R.string.version_copied)
                }
            }
            return false
        }


    }

    companion object {
        internal val TAG = Preferences::class.java.simpleName
        const val RESULT_LOGOUT = Activity.RESULT_FIRST_USER + 1
        const val RESULT_ARCHIVE = Activity.RESULT_FIRST_USER + 2
        const val RESULT_RECREATE_ACTIVITY = Activity.RESULT_FIRST_USER + 3
    }
}
