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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.FileObserver;
import android.preference.PreferenceManager;
import android.util.Log;
import nl.mpcjanssen.simpletask.task.LocalFileTaskRepository;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletaskdonate.R;


public class MainApplication extends Application {
    private final static String TAG = MainApplication.class.getSimpleName();
    public static Context appContext;
    public SharedPreferences m_prefs;
    private TaskBag taskBag;
    private FileObserver m_observer;

    public static Context getAppContext() {
        return appContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MainApplication.appContext = getApplicationContext();
        m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
        TaskBag.Preferences taskBagPreferences = new TaskBag.Preferences(
                m_prefs);
        LocalFileTaskRepository localTaskRepository = new LocalFileTaskRepository(taskBagPreferences);
        m_observer = new FileObserver(localTaskRepository.get_todo_file().getParent(),
        				FileObserver.ALL_EVENTS) {

			@Override
			public void onEvent(int event, String path) {
				if (path!=null && path.equals("todo.txt") ) {
                    Log.v(TAG, path + " event: " + event);
                    if( event == FileObserver.CLOSE_WRITE ||
                        event == FileObserver.MOVED_TO) {
					    Log.v(TAG, path + " modified reloading taskbag");
					    taskBag.reload();
					    updateUI();
                    }
				}
			}
		};
        m_observer.startWatching();
        localTaskRepository.setFileObserver(m_observer);
        this.taskBag = new TaskBag(taskBagPreferences, localTaskRepository);

        taskBag.reload();
    }

    public TaskBag getTaskBag() {
        return taskBag;
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

    public void showToast(int resid) {
        Util.showToastLong(this, resid);
    }

    public void showToast(String string) {
        Util.showToastLong(this, string);
    }

    /**
     * Update user interface
     *
     * Update the elements of the user interface. The listview with tasks will be updated
     * if it is visible (by broadcasting an intent). All widgets will be updated as well.
     * This method should be called whenever the TaskBag changes.
     */
    public void updateUI() {
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
}
