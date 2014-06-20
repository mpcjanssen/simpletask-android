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

import java.io.File;
import java.util.List;

import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.TaskIo;

public class DropboxRemoteClient implements RemoteClient {

    // Stubs for cloudless versions	
    public DropboxRemoteClient(Object todoApplication,
			Object sharedPreferences) {
	}

	@Override
	public Client getClient() {
		return null;
	}

	@Override
	public boolean authenticate() {
		return true;
	}

	@Override
	public void deauthenticate() {
	}

	@Override
	public boolean isAuthenticated() {
		return true;
	}

	@Override
	public PullTodoResult pullTodo() {
		return null;
	}

	@Override
	public void pushTodo(File todoFile, boolean overwrite) {
	}

    @Override
    public String getDonePath() {
        return null;
    }

    @Override
    public void append(String doneFile, List<Task> archiveTasks, boolean windowsLineBreak) throws Exception {
        TaskIo.writeToFile(archiveTasks, new File(doneFile), true, windowsLineBreak);
    }

    @Override
	public boolean startLogin() {
		return true;
	}

	@Override
	public boolean finishLogin() {
		return true;
	}

    public Object getApi() {
		return null;
	}
}
