/**
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * Copyright (c) 2013- Mark Janssen
 * Copyright (c) 2015 Vojtech Kral
 *
 * LICENSE:
 *
 * Simpletas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Simpletask is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Sinpletask.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
 * @copyright 2015 Vojtech Kral
 */
package nl.mpcjanssen.simpletask;

import android.annotation.SuppressLint;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.widget.EditText;
import de.greenrobot.dao.AbstractDao;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.dao.*;
import nl.mpcjanssen.simpletask.dao.gen.*;
import nl.mpcjanssen.simpletask.remote.BackupInterface;
import nl.mpcjanssen.simpletask.remote.FileStore;
import nl.mpcjanssen.simpletask.remote.FileStoreInterface;
import nl.mpcjanssen.simpletask.task.TodoList;
import nl.mpcjanssen.simpletask.util.Util;


import java.io.File;
import java.io.IOException;
import java.util.*;


public class TodoApplication extends Application implements

        SharedPreferences.OnSharedPreferenceChangeListener, TodoList.TodoListChanged, FileStoreInterface.FileChangeListener, BackupInterface {

    private static final String TAG = "TodoApplication";
    private static Context m_appContext;
    private static SharedPreferences m_prefs;
    private LocalBroadcastManager localBroadcastManager;


    private TodoList m_todoList;
    private CalendarSync m_calSync;
    private BroadcastReceiver m_broadcastReceiver;

    public static final boolean ATLEAST_API16 = android.os.Build.VERSION.SDK_INT >= 16;
    public static final boolean ATLEAST_API19 = android.os.Build.VERSION.SDK_INT >= 19;
    private int m_Theme = -1;
    private Logger log;
    private FileStore mFileStore;

    DaoSession daoSession;
    private DaoMaster.DevOpenHelper helper;
    private SQLiteDatabase todoDb;
    private DaoMaster daoMaster;
    public TodoListItemDao todoListItemDao;
    public TodoListStatusDao todoListStatusDao;
    public LogItemDao logDao;
    TodoFileDao backupDao;


    public static Context getAppContext() {
        return m_appContext;
    }

    public static SharedPreferences getPrefs() {
        return m_prefs;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        helper = new DaoMaster.DevOpenHelper(this, "TodoFiles_v1.db", null);
        todoDb = helper.getWritableDatabase();
        daoMaster = new DaoMaster(todoDb);
        daoSession = daoMaster.newSession();
        todoListItemDao = daoSession.getTodoListItemDao();
        todoListStatusDao = daoSession.getTodoListStatusDao();
        logDao = daoSession.getLogItemDao();
        backupDao = daoSession.getTodoFileDao();
        log = Logger.INSTANCE;
        log.setDao(logDao);

        log.debug(TAG, "onCreate()");


        TodoApplication.m_appContext = getApplicationContext();
        TodoApplication.m_prefs = PreferenceManager.getDefaultSharedPreferences(getAppContext());
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_UPDATE_UI);

        intentFilter.addAction(Constants.BROADCAST_FILE_WRITE_FAILED);
        m_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
            if (intent.getAction().equals(Constants.BROADCAST_UPDATE_UI)) {
                    m_calSync.syncLater();
                    redrawWidgets();
                    updateWidgets();
                } else if (intent.getAction().equals(Constants.BROADCAST_FILE_WRITE_FAILED)) {
                    Util.showToastLong(getApplicationContext(), R.string.write_failed);
                }
            }
        };
        localBroadcastManager.registerReceiver(m_broadcastReceiver, intentFilter);
        prefsChangeListener(this);
        m_todoList = new TodoList(this, this);
        this.mFileStore = new FileStore(this, this);
        log.info(TAG, "Created todolist {}" + m_todoList);
        loadTodoList(true);
        m_calSync = new CalendarSync(this, isSyncDues(), isSyncThresholds());
        scheduleOnNewDay();
    }

    private void scheduleOnNewDay () {
        // Schedules activities to run on a new day
        // - Refresh widgets and UI
        // - Cleans up logging

        Calendar calendar = Calendar.getInstance();



        // Prevent alarm from triggering for today when setting it
        calendar.add(Calendar.DATE, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 2);
        calendar.set(Calendar.SECOND, 0);


        Logger.INSTANCE.info(TAG, "Scheduling daily UI update alarm, first at " + calendar.getTime());
        PendingIntent pi = PendingIntent.getBroadcast(this, 0,
                new Intent(Constants.INTENT_NEW_DAY),PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager am = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        am.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,pi);
    }

    public void prefsChangeListener(SharedPreferences.OnSharedPreferenceChangeListener listener) {
        m_prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public LocalBroadcastManager getLocalBroadCastManager() {
        return localBroadcastManager;
    }

    @Override
    public void onTerminate() {
        log.info(TAG, "Deregistered receiver");
        m_prefs.unregisterOnSharedPreferenceChangeListener(this);
        if (m_broadcastReceiver!=null) {
            localBroadcastManager.unregisterReceiver(m_broadcastReceiver);
        }
        super.onTerminate();
    }


    public String[] getDefaultSorts () {
        return getResources().getStringArray(R.array.sortKeys);
    }

    public boolean showCompleteCheckbox() {
        return m_prefs.getBoolean(getString(R.string.ui_complete_checkbox), true);
    }

    public boolean showCalendar() {
        return m_prefs.getBoolean(getString(R.string.ui_show_calendarview), false);
    }

    public boolean showHidden() {
        return m_prefs.getBoolean(getString(R.string.show_hidden), false);
    }

    public boolean showEmptyLists() {
        return m_prefs.getBoolean(getString(R.string.show_empty_lists), true);
    }

    public String getListTerm() {
        if (useTodotxtTerms()) {
            return getString(R.string.context_prompt_todotxt);
        } else {
            return getString(R.string.context_prompt);
        }
    }

    public String getTagTerm() {
        if (useTodotxtTerms()) {
            return getString(R.string.project_prompt_todotxt);
        } else {
            return getString(R.string.project_prompt);
        }
    }

    private boolean useTodotxtTerms() {
        return m_prefs.getBoolean(getString(R.string.ui_todotxt_terms), false);
    }

    public boolean useFastScroll() {
        return m_prefs.getBoolean(getString(R.string.ui_fast_scroll), true);
    }

    // Use create date as backup for threshold date
    public boolean useCreateBackup() {
        return m_prefs.getBoolean(getString(R.string.use_create_backup), false);
    }

    public boolean showTxtOnly() {
        return m_prefs.getBoolean(getString(R.string.show_txt_only), false);
    }

    public boolean isSyncDues() {
        return ATLEAST_API16 && m_prefs.getBoolean(getString(R.string.calendar_sync_dues), false);
    }

    public boolean isSyncThresholds() {
        return ATLEAST_API16 && m_prefs.getBoolean(getString(R.string.calendar_sync_thresholds), false);
    }

    public int getReminderDays() {
        return m_prefs.getInt(getString(R.string.calendar_reminder_days), 1);
    }

    public int getReminderTime() {
        return m_prefs.getInt(getString(R.string.calendar_reminder_time), 720);
    }


    public String getTodoFileName() {
        String name =  m_prefs.getString(getString(R.string.todo_file_key), FileStore.getDefaultPath(this));
        File todoFile = new File(name);
        try {
            return todoFile.getCanonicalPath();
        } catch (IOException e) {
            return FileStore.getDefaultPath(this);
        }
    }

    public File getTodoFile() {
        return new File(getTodoFileName());
    }

    @SuppressLint("CommitPrefEdits")
    public void setTodoFile(String todo) {
        m_prefs.edit().putString(getString(R.string.todo_file_key), todo).commit();
    }

    public boolean isAutoArchive() {
        return m_prefs.getBoolean(getString(R.string.auto_archive_pref_key), false);
    }

    public boolean isBackSaving() {
        return m_prefs.getBoolean(getString(R.string.back_key_saves_key), false);
    }

    public boolean hasPrependDate() {
        return m_prefs.getBoolean(getString(R.string.prepend_date_pref_key), true);
    }

    public boolean hasKeepPrio() {
        return m_prefs.getBoolean(getString(R.string.keep_prio), true);
    }

    public String getShareAppendText() {
        return m_prefs.getString(getString(R.string.share_task_append_text), "");
    }

    public boolean hasCapitalizeTasks() {
        return m_prefs.getBoolean(getString(R.string.capitalize_tasks), false);
    }

    public boolean hasColorDueDates() {
        return m_prefs.getBoolean(getString(R.string.color_due_date_key), true);
    }

    public boolean hasLandscapeDrawers() {
        return (m_prefs.getBoolean(getString(R.string.ui_drawer_fixed_landscape), false) &&
                getResources().getBoolean(R.bool.is_landscape));
    }

    public void setEditTextHint(@NonNull EditText editText, int resid ) {
        if (m_prefs.getBoolean(getString(R.string.ui_show_edittext_hints), true)) {
            editText.setHint(resid);
        }
    }

    public boolean isAddTagsCloneTags() {
        return m_prefs.getBoolean(getString(R.string.clone_tags_key),false);
    }

    public void setAddTagsCloneTags(boolean bool) {
        m_prefs.edit()
                .putBoolean(getString(R.string.clone_tags_key),bool)
                .apply();
    }

    public boolean hasAppendAtEnd() {
        return m_prefs.getBoolean(getString(R.string.append_tasks_at_end),true);
    }

    public boolean isWordWrap() {
        return m_prefs.getBoolean(getString(R.string.word_wrap_key),true);
    }

    public boolean showTodoPath() {
        return m_prefs.getBoolean(getString(R.string.show_todo_path),false);
    }


    public boolean backClearsFilter() {
        return m_prefs.getBoolean(getString(R.string.back_clears_filter),false);
    }
    public boolean sortCaseSensitive() {
        return m_prefs.getBoolean(getString(R.string.ui_sort_case_sensitive),true);
    }

    @NonNull
    public String getEol() {
        if( m_prefs.getBoolean(getString(R.string.line_breaks_pref_key),true)) {
            return "\r\n";
        } else {
            return "\n";
        }
    }

    public boolean hasDonated() {
        try {
            getPackageManager().getInstallerPackageName("nl.mpcjanssen.simpletask.donate");
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    public boolean isLoading() {
        return getFileStore().isLoading();
    }


    public void setWordWrap(boolean bool) {
        m_prefs.edit()
                .putBoolean(getString(R.string.word_wrap_key), bool)
                .apply();
    }

    @NonNull
    public TodoList getTodoList() {
        return m_todoList;
    }

    public void loadTodoList(boolean background) {
        log.info(TAG, "Load todolist");
        m_todoList.reload(mFileStore, getTodoFileName(), this, localBroadcastManager, background, getEol());

    }


    public void fileChanged(@Nullable String newName) {
        if (newName!=null) {
            setTodoFile(newName);
        }
        loadTodoList(true);
    }


    public void updateWidgets() {
        AppWidgetManager mgr = AppWidgetManager.getInstance(getApplicationContext());
        for (int appWidgetId : mgr.getAppWidgetIds(new ComponentName(getApplicationContext(), MyAppWidgetProvider.class))) {
            mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widgetlv);
            log.info(TAG, "Updating widget: " + appWidgetId);
        }
    }

    private void redrawWidgets(){
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(this, MyAppWidgetProvider.class));
        log.info(TAG, "Redrawing widgets ");
        if (appWidgetIds.length > 0) {
            new MyAppWidgetProvider().onUpdate(this, appWidgetManager, appWidgetIds);
        }
    }

    private int themeStringToId (String theme) {
        switch (theme) {
            case "dark":
                return R.style.AppTheme;
            case "light_darkactionbar":
                return R.style.AppTheme_Light_DarkActionBar;
        }
        return R.style.AppTheme_Light_DarkActionBar;

    }

    public void reloadTheme() {
        m_Theme = -1;
    }
    public int getActiveTheme() {
        if (m_Theme != -1 ) {
            return m_Theme;
        }
        m_Theme = themeStringToId(getPrefs().getString(getString(R.string.theme_pref_key), ""));
        return m_Theme;

    }

    public boolean isDarkTheme() {
        switch (getActiveThemeString()) {
            case "dark":
                return true;
            default:
                return false;
        }
    }

    public boolean isDarkWidgetTheme() {
        return "dark".equals(getPrefs().getString(getString(R.string.widget_theme_pref_key), "light_darkactionbar"));
    }

    private String getActiveThemeString() {
        return getPrefs().getString(getString(R.string.theme_pref_key), "light_darkactionbar");
    }

    public boolean getFullDropboxAccess() {
        return getPrefs().getBoolean(getString(R.string.dropbox_full_access), true);
    }

    public void setFullDropboxAccess(boolean full) {
        getPrefs().edit().putBoolean(getString(R.string.dropbox_full_access), full).commit();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @NonNull String s) {
        if (s.equals(getString(R.string.widget_theme_pref_key)) ||
                s.equals(getString(R.string.widget_extended_pref_key)) ||
                s.equals(getString(R.string.widget_background_transparency)) ||
                s.equals(getString(R.string.widget_header_transparency))) {
            redrawWidgets();
        }  else if (s.equals(getString(R.string.calendar_sync_dues))) {
            m_calSync.setSyncDues(isSyncDues());
        } else if (s.equals(getString(R.string.calendar_sync_thresholds))) {
            m_calSync.setSyncThresholds(isSyncThresholds());
        } else if (s.equals(getString(R.string.calendar_reminder_days)) ||
                   s.equals(getString(R.string.calendar_reminder_time))) {
            m_calSync.syncLater();
        }
    }

    public void switchTodoFile(String newTodo, boolean background) {
        setTodoFile(newTodo);
        loadTodoList(background);

    }


    public void todoListChanged() {
        log.info(TAG, "Tasks have changed, update UI");
        localBroadcastManager.sendBroadcast(new Intent(Constants.BROADCAST_SYNC_DONE));
        localBroadcastManager.sendBroadcast(new Intent(Constants.BROADCAST_UPDATE_UI));


    }

    public int getActiveFont() {
        String fontsize =  getPrefs().getString("fontsize", "medium");
        switch (fontsize) {
            case "small":
                return R.style.FontSizeSmall;
            case "large":
                return R.style.FontSizeLarge;
            case "huge":
                return R.style.FontSizeHuge;
            default:
                return R.style.FontSizeMedium;
        }
    }

    @NonNull
    public FileStoreInterface getFileStore() {
        return mFileStore;
    }

    public void showConfirmationDialog(@NonNull Context cxt, int msgid,
                                              @NonNull DialogInterface.OnClickListener oklistener, int titleid) {
        boolean show = getPrefs().getBoolean(getString(R.string.ui_show_confirmation_dialogs), true);

        AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        builder.setTitle(titleid);
        builder.setMessage(msgid);
        builder.setPositiveButton(android.R.string.ok, oklistener);
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.setCancelable(true);
        Dialog dialog = builder.create();
        if (show) {
           dialog.show();
        } else {
            oklistener.onClick(dialog , DialogInterface.BUTTON_POSITIVE);
        }
    }

    public boolean isAuthenticated() {
        FileStoreInterface fs = getFileStore();
        return fs.isAuthenticated();
    }

    public void startLogin(LoginScreen loginScreen, int i) {
        getFileStore().startLogin(loginScreen,i);
    }

    public int storeType() {
        return getFileStore().getType();
    }

    public void browseForNewFile(@NonNull final Activity act) {
        FileStoreInterface fileStore = getFileStore();
        fileStore.browseForNewFile(
                act,
                new File(getTodoFileName()).getParent(),
                new FileStoreInterface.FileSelectedListener() {
                    @Override
                    public void fileSelected(String file) {
                        switchTodoFile(file, true);
                    }
                },
		showTxtOnly());
    }

    @NonNull
    public String getDoneFileName() {
        return new File(getTodoFile().getParentFile(), "done.txt").getAbsolutePath();
    }

    @Override
    public void backup(String name, String contents) {

        Date now = new Date();
        TodoFile fileToBackup = new TodoFile(contents, name, now);
        backupDao.insertOrReplace(fileToBackup);
        // Clean up old files
        Date removeBefore = new Date(now.getTime()-2*24*60*60*1000);

        backupDao.queryBuilder()
                .where(TodoFileDao.Properties.Date.lt(removeBefore))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();

    }

    public String getSortString(String key) {
        if (useTodotxtTerms()) {
            if ("by_context".equals(key)) {
                return getString(R.string.by_context_todotxt);
            }
            if ("by_project".equals(key)) {
                return getString(R.string.by_project_todotxt);
            }
        }
        List<String> keys = Arrays.asList(getResources().getStringArray(R.array.sortKeys));
        String[] values = getResources().getStringArray(R.array.sort);
        int index = keys.indexOf(key);
        if (index==-1) {
            return getString(R.string.none);
        }
        return values[index];
    }

    public boolean hasShareTaskShowsEdit() {
        return m_prefs.getBoolean(getString(R.string.share_task_show_edit), false);
    }
}
