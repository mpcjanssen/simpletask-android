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
package nl.mpcjanssen.simpletask;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import nl.mpcjanssen.simpletask.remote.RemoteClientManager;
import nl.mpcjanssen.simpletask.remote.RemoteConflictException;
import nl.mpcjanssen.simpletask.sort.*;
import nl.mpcjanssen.simpletask.task.LocalFileTaskRepository;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.todotxtholo.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class TodoApplication extends Application {
    private final static String TAG = TodoApplication.class.getSimpleName();
    public static Context appContext;
    public SharedPreferences m_prefs;
    public boolean m_pulling = false;
    public boolean m_pushing = false;
    private RemoteClientManager remoteClientManager;
    private TaskBag taskBag;
    private BroadcastReceiver m_broadcastReceiver;
    private Handler handler = new Handler();
    private Runnable runnable;

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        TodoApplication.appContext = getApplicationContext();
        m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        remoteClientManager = new RemoteClientManager(this, m_prefs);
        TaskBag.Preferences taskBagPreferences = new TaskBag.Preferences(
                m_prefs);
        LocalFileTaskRepository localTaskRepository = new LocalFileTaskRepository(taskBagPreferences);
        this.taskBag = new TaskBag(taskBagPreferences, localTaskRepository, remoteClientManager);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.INTENT_SET_MANUAL);
        intentFilter.addAction(Constants.INTENT_START_SYNC_WITH_REMOTE);
        intentFilter.addAction(Constants.INTENT_START_SYNC_TO_REMOTE);
        intentFilter.addAction(Constants.INTENT_START_SYNC_FROM_REMOTE);
        intentFilter.addAction(Constants.INTENT_ASYNC_FAILED);

        if (null == m_broadcastReceiver) {
            m_broadcastReceiver = new BroadcastReceiverExtension();
            registerReceiver(m_broadcastReceiver, intentFilter);
        }

        taskBag.reload();
        // Pull from dropbox every 5 minutes
        runnable = new Runnable() {
            @Override
            public void run() {
      /* do what you need to do */
                if (!isManualMode()) {
                    backgroundPullFromRemote();
                }
      /* reschedule next */
                handler.postDelayed(this, 5 * 60 * 1000);
                Log.v(TAG, "Pulling from remote");
            }
        };

        handler.postDelayed(runnable, 100);

    }

    @Override
    public void onTerminate() {
        unregisterReceiver(m_broadcastReceiver);
        super.onTerminate();
    }

    /**
     * If we previously tried to push and failed, then attempt to push again
     * now. Otherwise, pull.
     */
    private void syncWithRemote(boolean force) {
        taskBag.store();
        if (needToPush()) {
            Log.d(TAG, "needToPush = true; pushing.");
            pushToRemote(force, false);
        } else {
            Log.d(TAG, "needToPush = false; pulling.");
            pullFromRemote(force);
        }

    }

    /**
     * Check network status, then push.
     */
    private void pushToRemote(boolean force, boolean overwrite) {
        setNeedToPush(true);
        if (!force && isManualMode()) {
            Log.i(TAG, "Working offline, don't push now");
        } else if (!m_pulling) {
            Log.i(TAG, "Working online; should push if file revisions match");
            backgroundPushToRemote(overwrite);
        } else {
            Log.d(TAG, "app is pulling right now. don't start push."); // TODO
        }
    }

    /**
     * Check network status, then pull.
     */
    private void pullFromRemote(boolean force) {
        if (!force && isManualMode()) {
            Log.i(TAG, "Working offline, don't pull now");
            return;
        }

        setNeedToPush(false);

        if (!m_pushing) {
            Log.i(TAG, "Working online; should pull file");
            backgroundPullFromRemote();
        } else {
            Log.d(TAG, "app is pushing right now. don't start pull."); // TODO
            // remove
            // after
            // AsyncTask
            // bug
            // fixed
        }
    }

    public TaskBag getTaskBag() {
        return taskBag;
    }

    public RemoteClientManager getRemoteClientManager() {
        return remoteClientManager;
    }
    
    public boolean isManualMode() {
        return m_prefs.getBoolean(getString(R.string.manual_sync_pref_key), false);
    }

    public boolean isAutoArchive() {
        return m_prefs.getBoolean(getString(R.string.auto_archive_pref_key), false);
    }

    public int fontSizeDelta() {
        int val;
        String value =  m_prefs.getString(getString(R.string.font_size_delta_pref_key), "0");
        try
        {
            val = Integer.parseInt(value);
        }
        catch (NumberFormatException nfe)
        {
            // bad data - set to sentinel
            Log.v(TAG,nfe.getMessage());
            val = 5;
        }
        return val;

    }

    public float prioFontSize () {
        float defaultSize = Float.parseFloat (getResources ().getString (R.string.taskPrioTextSize));
        return (float)Math.max(5.0, defaultSize+fontSizeDelta());
    }

    public float taskTextFontSize () {
        float defaultSize = Float.parseFloat (getResources ().getString (R.string.taskTextSize));
        return (float)Math.max(5.0, defaultSize+fontSizeDelta());
    }

    public float taskAgeFontSize () {
        float defaultSize = Float.parseFloat (getResources ().getString (R.string.taskAgeTextSize));
        return (float)Math.max(5.0, defaultSize+fontSizeDelta());
    }

    public float headerFontSize () {
        float defaultSize = Float.parseFloat (getResources ().getString (R.string.headerTextSize));
        return (float)Math.max(5.0, defaultSize+fontSizeDelta());
    }

    public void setManualMode(boolean manual) {
    	Editor edit = m_prefs.edit();
        edit.putBoolean(getString(R.string.manual_sync_pref_key), manual);
        edit.commit();
    }

    public boolean needToPush() {
        return m_prefs.getBoolean(Constants.PREF_NEED_TO_PUSH, false);
    }

    public void setNeedToPush(boolean needToPush) {
        Editor editor = m_prefs.edit();
        editor.putBoolean(Constants.PREF_NEED_TO_PUSH, needToPush);
        editor.commit();
    }

    public void showToast(int resid) {
        Util.showToastLong(this, resid);
    }

    public void showToast(String string) {
        Util.showToastLong(this, string);
    }

    /**
     * Do asynchronous push with gui changes. Do availability check first.
     */
    void backgroundPushToRemote(final boolean overwrite) {
        if (getRemoteClientManager().getRemoteClient().isAuthenticated()) {
            Intent i = new Intent();
            i.setAction(Constants.INTENT_SYNC_START);
            sendBroadcast(i);
            m_pushing = true;
            updateUI();

            new AsyncTask<Void, Void, Integer>() {
                static final int SUCCESS = 0;
                static final int CONFLICT = 1;
                static final int ERROR = 2;

                @Override
                protected Integer doInBackground(Void... params) {
                    try {
                        Log.d(TAG, "start taskBag.pushToRemote");
                        taskBag.pushToRemote(true, overwrite);
                    } catch (RemoteConflictException c) {
                        Log.e(TAG, c.getMessage());
                        return CONFLICT;
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        return ERROR;
                    }
                    return SUCCESS;
                }

                @Override
                protected void onPostExecute(Integer result) {
                    Log.d(TAG, "post taskBag.pushToremote");
                    Intent i = new Intent();
                    i.setAction(Constants.INTENT_SYNC_DONE);
                    sendBroadcast(i);
                    if (result == SUCCESS) {
                        Log.d(TAG, "taskBag.pushToRemote done");
                        m_pushing = false;
                        setNeedToPush(false);
                        updateUI();
                        // Push is complete. Now do a pull in case the remote
                        // done.txt has changed.
                        pullFromRemote(true);
                    } else if (result == CONFLICT) {
                        // FIXME: need to know which file had conflict
                        sendBroadcast(new Intent(Constants.INTENT_SYNC_CONFLICT));
                    } else {
                        sendBroadcast(new Intent(Constants.INTENT_ASYNC_FAILED));
                    }
                    super.onPostExecute(result);
                }

            }.execute();

        } else {
            Log.e(TAG, "NOT AUTHENTICATED!");
            showToast("NOT AUTHENTICATED!");
        }

    }

    /**
     * Do an asynchronous pull from remote. Check network availability before
     * calling this.
     */
    private void backgroundPullFromRemote() {
        if (getRemoteClientManager().getRemoteClient().isAuthenticated()) {
            Intent i = new Intent();
            i.setAction(Constants.INTENT_SYNC_START);
            sendBroadcast(i);
            m_pulling = true;
            // Comment out next line to avoid resetting list position at top;
            // should maintain position of last action
            // updateUI();

            new AsyncTask<Void, Void, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    try {
                        Log.d(TAG, "start taskBag.pullFromRemote");
                        taskBag.pullFromRemote(true);
                    } catch (Exception e) {
                        Log.e(TAG, e.getMessage());
                        return false;
                    }
                    return true;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    Log.d(TAG, "post taskBag.pullFromRemote");
                    super.onPostExecute(result);
                    if (result) {
                        Log.d(TAG, "taskBag.pullFromRemote done");
                        m_pulling = false;
                        updateUI();
                    } else {
                        sendBroadcast(new Intent(Constants.INTENT_ASYNC_FAILED));
                    }
                    super.onPostExecute(result);
                    Intent i = new Intent();
                    i.setAction(Constants.INTENT_SYNC_DONE);
                    sendBroadcast(i);
                }

            }.execute();
        } else {
            Log.e(TAG, "NOT AUTHENTICATED!");
        }
    }

    /**
     * Update user interface
     *
     * Update the elements of the user interface. The listview with tasks will be updated
     * if it is visible (by broadcasting an intent). All widgets will be updated as well.
     * This method should be called whenever the TaskBag changes.
     */
    private void updateUI() {
        sendBroadcast(new Intent(Constants.INTENT_UPDATE_UI));
        updateWidgets();
    }

    public void updateWidgets() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
        for (int appWidgetId : mgr.getAppWidgetIds(new ComponentName(getApplicationContext(), MyAppWidgetProvider.class))) {
            mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetlv);
            Log.v(TAG, "Updating widget: " + appWidgetId);
        }
    }

    private final class BroadcastReceiverExtension extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean force_sync = intent.getBooleanExtra(
                    Constants.EXTRA_FORCE_SYNC, false);
            boolean overwrite = intent.getBooleanExtra(
                    Constants.EXTRA_OVERWRITE, false);
            if (intent.getAction().equalsIgnoreCase(
                    Constants.INTENT_START_SYNC_WITH_REMOTE)) {
                syncWithRemote(force_sync);
            } else if (intent.getAction().equalsIgnoreCase(
                    Constants.INTENT_START_SYNC_TO_REMOTE)) {
                pushToRemote(force_sync, overwrite);
            } else if (intent.getAction().equalsIgnoreCase(
                    Constants.INTENT_START_SYNC_FROM_REMOTE)) {
                pullFromRemote(force_sync);
            } else if (intent.getAction().equalsIgnoreCase(
                    Constants.INTENT_ASYNC_FAILED)) {
                showToast("Synchronizing Failed");
                m_pulling = false;
                m_pushing = false;
                updateUI();
            }
        }
    }

}
