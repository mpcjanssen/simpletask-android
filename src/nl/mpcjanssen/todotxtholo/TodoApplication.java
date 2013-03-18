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

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.FileObserver;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.dropbox.sync.android.*;
import nl.mpcjanssen.todotxtholo.task.Task;
import nl.mpcjanssen.todotxtholo.task.TaskBag;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TodoApplication extends Application implements
        DbxFileSystem.PathListener, DbxFileSystem.SyncStatusListener {

    public final static String TAG = TodoTxtTouch.class.getSimpleName();
    final int SYNC_NOTIFICATION_ID = 0x0;
    final int REQUEST_LINK_TO_DBX = 0x0;

    private DbxAccountManager mDbxAcctMgr;
    private final TaskBag mTaskBag = new TaskBag();
    private DbxFileSystem dbxFs;
    private DbxPath mTodoPath = new DbxPath("todo.txt");
    private DbxPath mDonePath = new DbxPath("done.txt");
    private SharedPreferences mPrefs;


    @Override
    public void onSyncStatusChange(DbxFileSystem dbxFileSystem) {
        try {
            DbxSyncStatus status = dbxFileSystem.getSyncStatus();
            Intent i;
            if (status.anyInProgress()) {
                Log.v(TAG, "Synchronizing with dropbox");
                showSyncNotification(true);
            } else {
                showSyncNotification(false);
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }

    private void showSyncNotification(boolean show) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.navigation_refresh)
                        .setContentTitle("Simpletask")
                        .setContentText("Synchronizing with dropbox");
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (show) {
            mNotificationManager.notify(SYNC_NOTIFICATION_ID, mBuilder.build());
        } else {
            mNotificationManager.cancel(SYNC_NOTIFICATION_ID);
        }
    }

    public DbxAccountManager getDbxAcctMgr() {
        return mDbxAcctMgr;
    }

    public TaskBag getTaskBag() {
        return mTaskBag;
    }

    public DbxFile createOrOpenFile(DbxPath path) {
        try {
            if (dbxFs == null) {
                dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            }
            if (!dbxFs.hasSynced()) {
                dbxFs.awaitFirstSync();
            }
            if (!dbxFs.isFile(path)) {
                Log.v(TAG, "file: " + path + " doesn't exist, creating");
                return dbxFs.create(path);
            } else {
                return dbxFs.open(path);
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void initTaskBag(TaskBag bag , DbxPath path) {
        Log.v(TAG, "Initializing TaskBag from dropbox");
        DbxFile mTodoFile;
        // Initialize the taskbag
        try {
            mTodoFile = createOrOpenFile(path);
            // Reflect changes we might have missed
            mTodoFile.update();
            bag.init(mTodoFile.readString());
            mTodoFile.close();
        } catch (DbxException.Unauthorized unauthorized) {
            unauthorized.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(),
                getString(R.string.dropbox_consumer_key), getString(R.string.dropbox_consumer_secret));
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public boolean isAuthenticated() {
        return mDbxAcctMgr.hasLinkedAccount();
    }

    public void storeTaskbag(TaskBag bag, DbxPath path) {
        try {
            DbxFile mTodoFile = createOrOpenFile(path);
            mTodoFile.writeString(bag.getTodoContents(getLineBreak()));
            mTodoFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        dbxFs.shutDown();
        mDbxAcctMgr.unlink();
    }

    public void loginDone() {
        try {
            dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            initTaskBag(mTaskBag, mTodoPath);
        } catch (DbxException.Unauthorized unauthorized) {
            unauthorized.printStackTrace();
        }

    }

    public void updateFromDropbox (boolean watch) {
        try {
            if (!mDbxAcctMgr.hasLinkedAccount()) {
                Log.v(TAG, "Unauthenticated: Not changing Dropbox handlers to: " + watch);
                return;
            }
            if (dbxFs == null) {
                dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            }
            if (watch) {
                Log.v(TAG, "Registering Dropbox handlers: " + watch);
                dbxFs.addSyncStatusListener(this);
                dbxFs.addPathListener(this, mTodoPath, DbxFileSystem.PathListener.Mode.PATH_ONLY);
                // Download pending changes we missed
                initTaskBag(mTaskBag, mTodoPath);
            } else {
                Log.v(TAG, "Registering Dropbox handlers: " + watch);
                dbxFs.removeSyncStatusListener(this);
                dbxFs.removePathListener(this, mTodoPath, DbxFileSystem.PathListener.Mode.PATH_ONLY);
                showSyncNotification(false);
            }
        } catch (DbxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPathChange(DbxFileSystem dbxFileSystem, DbxPath dbxPath, Mode mode) {
        Log.v(TAG, "File changed on dropbox reloading: " + dbxPath.getName());
        // Don't reload the taskbag here.
        // It will result in File already open errors from the Sync API
        Intent i = new Intent(Constants.INTENT_RELOAD_TASKBAG);
        sendBroadcast(i);
    }

    public String getDefaultSort() {
        String[] sortValues = getResources().getStringArray(R.array.sortValues);
        return mPrefs.getString(getString(R.string.default_sort_pref_key), "sort_by_context");
    }

    public String getLineBreak() {
        if (mPrefs.getBoolean(getString(R.string.windows_line_breaks_pref_key), true)) {
          return "\r\n";
        } else {
          return "\n";
        }
    }

    public void archiveTasks() {
        List<Task> completedTasks = getTaskBag().completedTasks();
        // Create a new completed taskbag
        // Dont't update tasks while archiving
        updateFromDropbox(false);
        TaskBag doneBag = new TaskBag();
        initTaskBag(doneBag, mDonePath);
        doneBag.addTasks(completedTasks);
        mTaskBag.deleteTasks(completedTasks);
        storeTaskbag(doneBag,mDonePath);
        storeTaskbag();
        updateFromDropbox(true);
    }

    public void initTaskBag() {
        this.initTaskBag(mTaskBag,mTodoPath);
    }

    public void storeTaskbag() {
        storeTaskbag(mTaskBag,mTodoPath);
    }
}
