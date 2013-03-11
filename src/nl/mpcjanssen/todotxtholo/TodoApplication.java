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


public class TodoApplication extends Application {
    final static String TAG = TodoTxtTouch.class.getSimpleName();

    private Context mAppContext;
    private DbxAccountManager mDbxAcctMgr;
    private TaskBag mTaskBag;
    private DbxFile mTodoFile;
    private DbxPath mTodoPath = new DbxPath("todo.txt");
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
            mTodoFile = dbxFs.open(mTodoPath);
            mTaskBag = new TaskBag(mTodoFile.readString());
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
            mTodoFile.writeString(mTaskBag.getTodoContents());
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void logout() {
        mDbxAcctMgr.unlink();
    }
}
