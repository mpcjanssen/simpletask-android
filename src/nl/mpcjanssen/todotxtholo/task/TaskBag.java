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
package nl.mpcjanssen.todotxtholo.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.mpcjanssen.todotxtholo.Constants;
import nl.mpcjanssen.todotxtholo.TodoTxtTouch;
import nl.mpcjanssen.todotxtholo.util.TaskIo;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxException.Unauthorized;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxFileSystem.PathListener;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxPath.InvalidPathException;


/**
 * Implementation of the TaskBag
 *
 * @author Tim Barlotta, Mark Janssen
 *         <p/>
 *         The taskbag is the backing store for the task list used by the application
 *         It is loaded from and stored to the local copy of the todo.txt file and
 *         it is global to the application so all activities operate on the same copy
 */
public class TaskBag implements PathListener {
    final static String TAG = TodoTxtTouch.class.getSimpleName();
    
    
    private Preferences preferences;
    private ArrayList<Task> tasks = new ArrayList<Task>();

	private DbxAccountManager dbxAcctMgr;
	private DbxFileSystem dbxFs;


	private ArrayList<Activity> obs = new ArrayList<Activity>();

    public TaskBag(Preferences taskBagPreferences, DbxAccountManager dbxAcctMgr) {
        this.preferences = taskBagPreferences;
        this.dbxAcctMgr = dbxAcctMgr;

    }


    public void listenForDropboxChanges(boolean listen) {
    	Log.v(TAG, "App is listening for background DropBox changes: " + listen );
    	if (listen) {
  		  dbxFs.addPathListener(this, new DbxPath("todo.txt"),
		            DbxFileSystem.PathListener.Mode.PATH_ONLY);
    	} else {
  		  dbxFs.removePathListener(this, new DbxPath("todo.txt"),
		            DbxFileSystem.PathListener.Mode.PATH_ONLY);
    	}
    	
    }
    public int size() {
        return tasks.size();
    }

    public ArrayList<Task> getTasks() {
        return tasks;
    }
    
    public void reload () {
    	try {
			dbxFs = DbxFileSystem.forAccount(dbxAcctMgr.getLinkedAccount());
			if (!dbxFs.hasSynced()) {
				dbxFs.awaitFirstSync();
			}
		} catch (Unauthorized e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DbxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	loadTodoFile();
    	dataSetChanged();
    }

    public Task getTaskAt(int position) {
        return tasks.get(position);
    }

    public void addAsTask(String input) {
        try {
            Task task = new Task(tasks.size(), input,
                    (preferences.isPrependDateEnabled() ? new Date() : null));
            reload();
            tasks.add(task);
            store();
        } catch (Exception e) {
            throw new TaskPersistException("An error occurred while adding {"
                    + input + "}", e);
        }
    }

    public void updateTask(Task task, String input) {
        task.init(input, null);
        store();
    }

    public Task find(Task task) {
        Task found = TaskBag.find(tasks, task);
        return found;
    }

	public void deleteTasks(List<Task> toDelete) {
		for (Task task : toDelete) {
			Task found = TaskBag.find(tasks, task);
			if (found != null) {
				tasks.remove(found);
			} else {
				throw new TaskPersistException("Task not found, not deleted");
			}
		}
		store();
	}

    private void saveTodoFile() {
	    	DbxFile todoFile;
	    	try {
				if (dbxFs.isFile(new DbxPath("todo.txt"))) {
					todoFile = dbxFs.open(new DbxPath("todo.txt"));
				} else {
					todoFile = dbxFs.create(new DbxPath("todo.txt"));
				}
		        TaskIo.writeTasksToFile(tasks, todoFile);
		        todoFile.close();
			} catch (InvalidPathException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DbxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }

    private void loadTodoFile() {
    	DbxFile todoFile;
    	try {
			if (dbxFs.isFile(new DbxPath("todo.txt"))) {
				todoFile = dbxFs.open(new DbxPath("todo.txt"));
			} else {
				todoFile = dbxFs.create(new DbxPath("todo.txt"));
			}
			tasks = TaskIo.loadTasksFromFile(todoFile);
	        todoFile.close();
		} catch (InvalidPathException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DbxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

    }

    public ArrayList<Priority> getPriorities() {
        // TODO cache this after reloads?
        Set<Priority> res = new HashSet<Priority>();
        for (Task item : tasks) {
            res.add(item.getPriority());
        }
        ArrayList<Priority> ret = new ArrayList<Priority>(res);
        Collections.sort(ret);
        return ret;
    }

    public ArrayList<String> getContexts() {
        // TODO cache this after reloads?
        Set<String> res = new HashSet<String>();
        for (Task item : tasks) {
            res.addAll(item.getContexts());
        }
        ArrayList<String> ret = new ArrayList<String>(res);
        Collections.sort(ret);
        ret.add(0, "-");
        return ret;
    }

    public ArrayList<String> getProjects() {
        // TODO cache this after reloads?
        Set<String> res = new HashSet<String>();
        for (Task item : tasks) {
            res.addAll(item.getProjects());
        }
        ArrayList<String> ret = new ArrayList<String>(res);
        Collections.sort(ret);
        ret.add(0, "-");
        return ret;
    }

    private static Task find(List<Task> tasks, Task task) {
        for (Task task2 : tasks) {
            if (task2 == task || (task2.getText().equals(task.getOriginalText())
                    && task2.getPriority() == task.getOriginalPriority())) {
                return task2;
            }
        }
        return null;
    }

    public static class Preferences {
        private final SharedPreferences sharedPreferences;

        public Preferences(SharedPreferences sharedPreferences) {
            this.sharedPreferences = sharedPreferences;
        }


        public boolean isPrependDateEnabled() {
            return sharedPreferences.getBoolean("todotxtprependdate", true);
        }

    }

	public void store() {
		saveTodoFile();		
		dataSetChanged();
	}
	
	public void archive() {
		// Read current done
		ArrayList<Task> incompleteTasks = new ArrayList<Task>();
    	DbxFile doneFile;
    	String contents = "";
    	try {
			if (dbxFs.isFile(new DbxPath("done.txt"))) {
				doneFile = dbxFs.open(new DbxPath("done.txt"));
				contents =  doneFile.readString();
			} else {
				doneFile = dbxFs.create(new DbxPath("done.txt"));
			}
	        
			for (Task task : tasks) {
				if (task.isCompleted()) {
					contents = contents + "\n" + task.inFileFormat();
				} else {
					incompleteTasks.add(task);
				}
			}
			doneFile.writeString(contents);
	        doneFile.close();
	        tasks = incompleteTasks;
	        store();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}


	@Override
	public void onPathChange(DbxFileSystem arg0, DbxPath arg1, Mode arg2) {
		Log.v(TAG, "Dropbox file changed, reloading");
		loadTodoFile();
		dataSetChanged();
	}

	
	public void dataSetChanged () {
		for (Activity ob : obs) {
			// TODO Auto-generated method stub
			Intent i = new Intent();
			i.setAction(Constants.INTENT_UPDATE_UI);
			ob.sendBroadcast(i);
		}	
	}
	public void registerDataSetObserver(Activity act) {
		obs.add(act);		
	}
	
	public void unRegisterDataSetObserver(Activity act) {
		obs.remove(act);		
	}
}
