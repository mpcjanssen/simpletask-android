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
package nl.mpcjanssen.simpletask.task;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.TodoException;
import nl.mpcjanssen.simpletask.remote.DropboxRemoteClient;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.exception.DropboxServerException;


/**
 * A task repository for interacting with the local file system
 * 
 * @author Tim Barlotta
 */
public class LocalFileTaskRepository implements LocalTaskRepository {
	private  final String TAG = LocalFileTaskRepository.class
			.getSimpleName();
	final File TODO_TXT_FILE;
	private final TaskBag.Preferences preferences;
    private final TodoApplication m_app;

    public LocalFileTaskRepository(TodoApplication app, File todo,  TaskBag.Preferences preferences) {
        this.m_app = app;
		this.preferences = preferences;
        this.TODO_TXT_FILE = todo;
	}

    public File getTodoTxtFile() {
        return TODO_TXT_FILE;
    }

	@Override
    public void init() {
		try {
			if (!TODO_TXT_FILE.exists()) {
				Util.createParentDirectory(TODO_TXT_FILE);
				TODO_TXT_FILE.createNewFile();
			}
		} catch (IOException e) {
			throw new TodoException("Error initializing LocalFile", e);
		}
	}

	@Override
    public ArrayList<Task> load() {
		init();
		if (!TODO_TXT_FILE.exists()) {
			Log.w(TAG, TODO_TXT_FILE.getAbsolutePath() + " does not exist!");
			throw new TodoException(TODO_TXT_FILE.getAbsolutePath()
					+ " does not exist!");
		} else {
			try {
				return TaskIo.loadTasksFromFile(TODO_TXT_FILE, preferences);
			} catch (IOException e) {
				throw new TodoException("Error loading from local file", e);
			}
		}
	}
    
    @Override
    public void store(ArrayList<Task> tasks) {
	m_app.stopWatching();
	TaskIo.writeToFile(tasks, TODO_TXT_FILE,
			   preferences.isUseWindowsLineBreaksEnabled());
		m_app.startWatching();
    }
    
    @Override
    public boolean archive(ArrayList<Task> tasks, List<Task> tasksToArchive, String doneFile)  {
        m_app.stopWatching();
	final boolean windowsLineBreaks = preferences.isUseWindowsLineBreaksEnabled();

	ArrayList<Task> archivedTasks = new ArrayList<Task>();
	final ArrayList<Task> remainingTasks = new ArrayList<Task>();

        for (Task task : tasks) {
            if (tasksToArchive!=null) {
                // Archive selected tasks
                if (tasksToArchive.indexOf(task)!=-1) {
                    archivedTasks.add(task);
                } else {
                    remainingTasks.add(task);
                }
            } else {
                // Archive completed tasks
                if (task.isCompleted()) {
                    archivedTasks.add(task);
                } else {
                    remainingTasks.add(task);
                }
            }
        }
        boolean result = true;
        try {
            DropboxRemoteClient remote = (DropboxRemoteClient) m_app.getRemoteClientManager().getRemoteClient();
            DropboxAPI api = remote.getApi();
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
            if (preferences.isUseWindowsLineBreaksEnabled()) {
                newContents = Util.join(currentArchive,"\r\n");
            } else {
                newContents = Util.join(currentArchive,"\n");
            }
            InputStream is = new ByteArrayInputStream(newContents.getBytes());
            DropboxAPI.UploadRequest req = api.putFileOverwriteRequest(doneFile, is, newContents.getBytes().length,null);
            req.upload();
            TaskIo.writeToFile(remainingTasks, TODO_TXT_FILE, false,
                    windowsLineBreaks);
        } catch (Exception e) {
            result = false;
            Log.v(TAG, e.toString());
        }
        m_app.startWatching();
	    return result;
    }

	@Override
    public boolean todoFileModifiedSince(Date date) {
		long date_ms = 0l;
		if (date != null) {
			date_ms = date.getTime();
		}
		return date_ms < TODO_TXT_FILE.lastModified();
	}

}
