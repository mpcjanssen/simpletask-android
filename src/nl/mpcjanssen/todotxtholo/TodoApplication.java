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

import com.dropbox.sync.android.*;

import android.app.Application;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import nl.mpcjanssen.todotxtholo.task.Task;
import nl.mpcjanssen.todotxtholo.task.TaskBag;
import nl.mpcjanssen.todotxtholo.util.Util;

import java.io.IOException;
import java.util.ArrayList;


public class TodoApplication extends Application implements DbxFileSystem.PathListener {
    final static String TAG = TodoTxtTouch.class.getSimpleName();

    private Context mAppContext;
    private DbxAccountManager mDbxAcctMgr;
    private TaskBag mTaskBag;
    private DbxPath mTodoPath = new DbxPath("todo.txt");
    private DbxPath mDonePath = new DbxPath("done.txt");
    private DbxFileSystem dbxFs ;

    public Context getAppContext() {
        return mAppContext;
    }

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
                mTaskBag = new TaskBag();
                mTaskBag.init(mTodoFile.readString());
                mTodoFile.close();
            }
        } catch (DbxException.Unauthorized unauthorized) {
            unauthorized.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mAppContext = getApplicationContext();
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
        String output = "";
        try {
            synchronized (this) {
                DbxFile mTodoFile = dbxFs.open(mTodoPath);
                mTodoFile.writeString(mTaskBag.getTodoContents());
                mTodoFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void logout() {
        mDbxAcctMgr.unlink();
    }

    public void archive() {
        // Read current done
        ArrayList<Task> completeTasks = new ArrayList<Task>();
        DbxFile doneFile;
        String contents = "";
        try {
            for (Task task : mTaskBag.getTasks()) {
                if (task.isCompleted()) {
                    completeTasks.add(task);
                    contents = contents + "\n" + task.inFileFormat();
                }
            }
            if (dbxFs.isFile(mDonePath)) {
                doneFile = dbxFs.open(mDonePath);
                contents =  doneFile.readString();
            } else {
                doneFile = dbxFs.create(mDonePath);
            }
            doneFile.writeString(contents);
            doneFile.close();
            mTaskBag.deleteTasks(completeTasks);
            storeTaskbag();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void watchDropbox(boolean watch) {
        Log.v(TAG, "Watching for dropbox changes: " + watch);
        if (dbxFs == null) {
            return;
        }
        if (watch) {
            dbxFs.addPathListener(this, mTodoPath, DbxFileSystem.PathListener.Mode.PATH_ONLY);
        }  else {
            dbxFs.addPathListener(this, mTodoPath, DbxFileSystem.PathListener.Mode.PATH_ONLY);
        }
    }

    @Override
    public void onPathChange(DbxFileSystem dbxFileSystem, DbxPath dbxPath, Mode mode) {
        Log.v(TAG, "File changed on dropbox reloading");
        Intent i = new Intent(Constants.INTENT_RELOAD_TASKBAG);
        sendBroadcast(i);
    }
}
