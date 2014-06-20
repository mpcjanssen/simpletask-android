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
package nl.mpcjanssen.simpletask.remote;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.Simpletask;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletask.R;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxServerException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;

public class DropboxRemoteClient implements RemoteClient {
	final static String TAG = Simpletask.class.getSimpleName();
	
	private static final String DEFAULT_TODO_TXT_REMOTE_PATH = "/todo/todo.txt";
	private static final AccessType ACCESS_TYPE = AccessType.DROPBOX;
	private static final File TODO_TXT_TMP_FILE = new File(
			TodoApplication.getAppContext().getFilesDir(),
			"tmp/todo.txt");

	private DropboxAPI<AndroidAuthSession> dropboxApi;
	private TodoApplication todoApplication;
	private SharedPreferences sharedPreferences;

	public DropboxRemoteClient(TodoApplication todoApplication,
			SharedPreferences sharedPreferences) {
		this.todoApplication = todoApplication;
		this.sharedPreferences = sharedPreferences;
	}

	@Override
	public Client getClient() {
		return Client.DROPBOX;
	}

	/**
	 * Get the stored key - secret pair for authenticating the user
	 * 
	 * @return a string array with key and secret
	 */
	private AccessTokenPair getStoredKeys() {
		String key = null;
		String secret = null;

		key = sharedPreferences.getString(Constants.PREF_ACCESSTOKEN_KEY, null);
		secret = sharedPreferences.getString(Constants.PREF_ACCESSTOKEN_SECRET,
				null);
		if (key != null && secret != null) {
			return new AccessTokenPair(key, secret);
		}
		return null;
	}

	/**
	 * Store the key - secret pair for an authenticated user.
	 * 
	 * @param accessTokenKey
	 * @param accessTokenSecret
	 */
	void storeKeys(String accessTokenKey, String accessTokenSecret) {
		Editor editor = sharedPreferences.edit();
		editor.putString(Constants.PREF_ACCESSTOKEN_KEY, accessTokenKey);
		editor.putString(Constants.PREF_ACCESSTOKEN_SECRET, accessTokenSecret);
		editor.commit();
	}

	/**
	 * Clear the stored keys, either because they are bad, or user has requested
	 * it
	 */
	private void clearAuthToken() {
		Editor editor = sharedPreferences.edit();
		editor.remove(Constants.PREF_ACCESSTOKEN_KEY);
        editor.remove(Constants.PREF_ACCESSTOKEN_SECRET);
		editor.commit();
	}

	@Override
	public boolean authenticate() {
		String consumerKey = todoApplication.getResources()
				.getText(R.string.dropbox_consumer_key).toString();
		consumerKey = consumerKey.replaceFirst("^db-","");
		String consumerSecret = todoApplication.getText(
				R.string.dropbox_consumer_secret).toString();

		AppKeyPair appKeys = new AppKeyPair(consumerKey, consumerSecret);
		AndroidAuthSession session = new AndroidAuthSession(appKeys,
				ACCESS_TYPE);
		dropboxApi = new DropboxAPI<AndroidAuthSession>(session);

		AccessTokenPair access = getStoredKeys();
		if (access != null) {
			dropboxApi.getSession().setAccessTokenPair(access);
		}
		return true;
	}

	@Override
	public void deauthenticate() {
		clearAuthToken();
		dropboxApi.getSession().unlink();
		TODO_TXT_TMP_FILE.delete();
	}

	@Override
	public boolean isAuthenticated() {
		return dropboxApi.getSession().isLinked();
	}

	/**
	 * Store the current 'rev' from the metadata retrieved from Dropbox.
	 * 
	 * @param key
	 *            Name of the key in sharedPreferences under which to store the
	 *            rev value.
	 * @param rev
	 *            The value of the rev to be stored.
	 */
	private void storeRev(String key, String rev) {
		Log.d(TAG, "Storing rev. key=" + key + ". val=" + rev);
		Editor prefsEditor = sharedPreferences.edit();
		prefsEditor.putString(key, rev);
		if (!prefsEditor.commit()) {
			Log.e(TAG, "Failed to store rev key! key=" + key + ". val=" + rev);
		}
	}

	/**
	 * Load the last 'rev' stored from Dropbox.
	 * 
	 * @param key
	 *            Name of the key in sharedPreferences from which to retrieve
	 *            the rev value.
	 * @return The value of the rev to be retrieved.
	 */
	private String loadRev(String key) {
		return sharedPreferences.getString(key, null);
	}

	@Override
	public PullTodoResult pullTodo() {
		DropboxFile todoFile = new DropboxFile(
				getTodoFile(), TODO_TXT_TMP_FILE,
				loadRev(Constants.PREF_TODO_REV));
		ArrayList<DropboxFile> dropboxFiles = new ArrayList<DropboxFile>(2);
		dropboxFiles.add(todoFile);
		
		DropboxFileDownloader downloader = new DropboxFileDownloader(
				dropboxApi, dropboxFiles);
		downloader.pullFiles();

		File downloadedTodoFile = null;
		if (todoFile.getStatus() == DropboxFileStatus.SUCCESS) {
			downloadedTodoFile = todoFile.getLocalFile();
			storeRev(Constants.PREF_TODO_REV, todoFile.getLoadedMetadata().rev);
		}

		return new PullTodoResult(downloadedTodoFile);
	}

	@Override
	public void pushTodo(File todoFile, boolean overwrite) {
		ArrayList<DropboxFile> dropboxFiles = new ArrayList<DropboxFile>(2);
		if (todoFile != null) {
			dropboxFiles.add(new DropboxFile(
					getTodoFile(), todoFile,
					loadRev(Constants.PREF_TODO_REV)));
		}


		DropboxFileUploader uploader = new DropboxFileUploader(dropboxApi,
				dropboxFiles, overwrite);
		uploader.pushFiles();

		if (uploader.getStatus() == DropboxFileStatus.SUCCESS) {
			if (dropboxFiles.size() > 0) {
				DropboxFile todoDropboxFile = dropboxFiles.get(0);
				if (todoDropboxFile.getStatus() == DropboxFileStatus.SUCCESS) {
					storeRev(Constants.PREF_TODO_REV,
							todoDropboxFile.getLoadedMetadata().rev);
				}
			}
		}
	}

	@Override
	public boolean startLogin() {
		dropboxApi.getSession().startAuthentication(
				todoApplication.getApplicationContext());
		return true;
	}

	@Override
	public boolean finishLogin() {
		if (dropboxApi.getSession().authenticationSuccessful()) {
			try {
				dropboxApi.getSession().finishAuthentication();

				AccessTokenPair tokens = dropboxApi.getSession()
						.getAccessTokenPair();

				storeKeys(tokens.key, tokens.secret);
			} catch (IllegalStateException e) {
				Log.i("DbAuthLog", "Error authenticating", e);
				return false;
			}
			return true;
		} else {
			Log.w("DbAuthLog", "Dropbox authentication failed");
		}
		return false;
	}

	DropboxAPI<AndroidAuthSession> getAPI() {
		return dropboxApi;
	}

	String getTodoFile() {
        String rawPath =  sharedPreferences.getString("todo_file", null);
        if (rawPath == null)  {
            rawPath = DEFAULT_TODO_TXT_REMOTE_PATH;
            sharedPreferences.edit().putString("todo_file", rawPath).commit();
        }
        // Return a proper path name otherwise syncing with dropbox will not work
        // Fixes bug #2
        rawPath = rawPath.replaceAll("/$", "");
        rawPath = rawPath.replaceAll("//", "/");
        return rawPath;
    }

    @Override
    public String getDonePath() {
        String todoPath = getTodoFile();
        File donePath = new File(todoPath).getParentFile();
        File doneFile = new File(donePath, "done.txt");
        try {
            return doneFile.getCanonicalPath();
        } catch (IOException e) {
            // Should never happen
            Log.e("DROPBOX", e.getMessage());
            return null;
        }
    }

    @Override
    public void append( String doneFile, List<Task> archivedTasks, boolean windowsLineBreak)  throws Exception  {
        DropboxAPI api = getApi();
        ArrayList<String> currentArchive = new ArrayList<String>();
        try {
            DropboxAPI.DropboxInputStream dbxStream = api.getFileStream(doneFile, null);
            currentArchive.addAll(TaskIo.loadTasksFromStream(dbxStream));
        } catch (DropboxServerException e) {
            if (e.error != 404) {
                // Rethrow if error is not equal to file not found
                throw e;
            }
        }
        for (Task t: archivedTasks) {
            currentArchive.add(t.inFileFormat());
        }
        String newContents;
        if (windowsLineBreak) {
            newContents = Util.join(currentArchive,"\r\n");
        } else {
            newContents = Util.join(currentArchive,"\n");
        }
        InputStream is = new ByteArrayInputStream(newContents.getBytes());
        DropboxAPI.UploadRequest req = api.putFileOverwriteRequest(doneFile, is, newContents.getBytes().length,null);
        req.upload();

    }

    public DropboxAPI getApi() {
        return dropboxApi;
    }

}
