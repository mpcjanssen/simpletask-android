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

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.dropbox.sync.android.*;
import nl.mpcjanssen.todotxtholo.task.TaskBag;

import java.io.IOException;


public class TodoApplication extends Application implements
        DbxFileSystem.PathListener, DbxFileSystem.SyncStatusListener {

    public final static String TAG = TodoTxtTouch.class.getSimpleName();
    final int SYNC_NOTIFICATION_ID = 0x0;

    @Override
    public void onSyncStatusChange(DbxFileSystem dbxFileSystem) {
        try {
            DbxSyncStatus status = dbxFileSystem.getSyncStatus();
            Intent i ;
            if (status.anyInProgress()) {
                Log.v(TAG, "Synchronizing with dropbox");
                showSyncNotification(true);
            }  else {
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

    private DbxAccountManager mDbxAcctMgr;
    private TaskBag mTaskBag;
    private DbxPath mTodoPath = new DbxPath("todo.txt");
    private DbxFileSystem dbxFs ;

    public DbxAccountManager getDbxAcctMgr() {
        return mDbxAcctMgr;
    }

    public TaskBag getTaskBag() {
        return mTaskBag;
    }

    public void initTaskBag() {
        Log.v(TAG, "Initializing TaskBag from dropbox");
        // Initialize the taskbag
        try {
            dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            dbxFs.awaitFirstSync();
            synchronized (this) {
                DbxFile mTodoFile;
                if (dbxFs.isFile(mTodoPath)) {
                    mTodoFile = dbxFs.open(mTodoPath);
                } else {
                    mTodoFile = dbxFs.create(mTodoPath);
                }
                // Reflect changes we might have missed
                mTodoFile.update();
                mTaskBag = new TaskBag();
                mTaskBag.init(mTodoFile.readString());
                mTodoFile.close();
            }
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
        if (mDbxAcctMgr.hasLinkedAccount()) {
            initTaskBag();
        }
    }

    public boolean isAuthenticated() {
        return mDbxAcctMgr.hasLinkedAccount();
    }

    public void storeTaskbag() {
        try {
            synchronized (this) {
                DbxFile mTodoFile = dbxFs.open(mTodoPath);
                mTodoFile.writeString(mTaskBag.getTodoContents());
                mTodoFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        mDbxAcctMgr.unlink();
    }

    public void watchDropbox(boolean watch) {
        Log.v(TAG, "Actively monitoring for dropbox changes: " + watch);
        if (dbxFs == null) {
            return;
        }
        try {
            if (watch) {
                dbxFs.addSyncStatusListener(this);
                dbxFs.addPathListener(this, mTodoPath, DbxFileSystem.PathListener.Mode.PATH_ONLY);
            } else {
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
}
