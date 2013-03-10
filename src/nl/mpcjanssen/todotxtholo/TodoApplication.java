/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 *
 * LICENSE:
 *
 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
package nl.mpcjanssen.todotxtholo;

import com.dropbox.sync.android.DbxAccountManager;

import android.app.Application;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import nl.mpcjanssen.todotxtholo.task.TaskBag;
import nl.mpcjanssen.todotxtholo.util.Util;


public class TodoApplication extends Application {
    private final static String TAG = TodoApplication.class.getSimpleName();
    public SharedPreferences m_prefs;
    public boolean m_pulling = false;
    public boolean m_pushing = false;
    private TaskBag taskBag;
    private BroadcastReceiver m_broadcastReceiver;
    public static Context appContext;
	private DbxAccountManager mDbxAcctMgr;

    @Override
    public void onCreate() {
        super.onCreate();
        TodoApplication.appContext = getApplicationContext();
        m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        TaskBag.Preferences taskBagPreferences = new TaskBag.Preferences(
                m_prefs);
		mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), 
				getString(R.string.dropbox_consumer_key), getString(R.string.dropbox_consumer_secret));

		this.taskBag = new TaskBag(taskBagPreferences, mDbxAcctMgr);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.INTENT_SET_MANUAL);
        intentFilter.addAction(Constants.INTENT_START_SYNC_WITH_REMOTE);
        intentFilter.addAction(Constants.INTENT_START_SYNC_TO_REMOTE);
        intentFilter.addAction(Constants.INTENT_START_SYNC_FROM_REMOTE);
        intentFilter.addAction(Constants.INTENT_ASYNC_FAILED);
        

    }

    public DbxAccountManager getmDbxAcctMgr() {
		return mDbxAcctMgr;
	}

	@Override
    public void onTerminate() {
        unregisterReceiver(m_broadcastReceiver);
        super.onTerminate();
    }



    public TaskBag getTaskBag() {
        return taskBag;
    }


    public static Context getAppContext() {
        return appContext;
    }

    public void showToast(int resid) {
        Util.showToastLong(this, resid);
    }

    public void showToast(String string) {
        Util.showToastLong(this, string);
    }

    public boolean completedLast() {
        return m_prefs.getBoolean(getString(R.string.sort_complete_last_pref_key), true);
    }

}
