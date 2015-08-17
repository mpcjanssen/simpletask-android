/**
 * This file is part of Simpletask.
 *
 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)
 * Copyright (c) 2013- Mark Janssen
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
 * @author Mark Janssen
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
 */
package nl.mpcjanssen.simpletask;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;
import nl.mpcjanssen.simpletask.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;


public class AddTaskBackground extends Activity {

    private TodoApplication m_app;

    private Logger log;

    @Override
    public void onCreate(Bundle instance) {
        log = LoggerFactory.getLogger(this.getClass());
        log.debug("onCreate()");
        super.onCreate(instance);
        m_app = (TodoApplication) this.getApplication();

        Intent intent = getIntent();
        final String action = intent.getAction();

        String append_text = m_app.getShareAppendText();

         if (Intent.ACTION_SEND.equals(action)) {
            log.debug("Share");
             String share_text;
             if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                Uri uri = (Uri) intent.getExtras().get(Intent.EXTRA_STREAM);
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    share_text = CharStreams.toString(new InputStreamReader(is, "UTF-8"));
                    is.close();
                } catch (IOException e) {
                    share_text = "";
                    e.printStackTrace();
                }

            } else if (intent.hasExtra(Intent.EXTRA_TEXT)) {
                share_text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT).toString();
            } else {
                share_text = "";
            }
            addBackgroundTask(share_text, append_text);
        } else if ("com.google.android.gm.action.AUTO_SEND".equals(action)) {
            // Called as note to self from google search/now
            noteToSelf(intent, append_text);

        } else if (Constants.INTENT_BACKGROUND_TASK.equals(action)) {
            log.debug("Adding background task");
            if (intent.hasExtra(Constants.EXTRA_BACKGROUND_TASK)) {
                addBackgroundTask(intent.getStringExtra(Constants.EXTRA_BACKGROUND_TASK),append_text);
            } else {
                log.warn("Task was not in extras");
            }

        }
    }

    private void startAddTaskActivity(List<Task> tasks) {
        log.info("Starting addTask activity");
        m_app.getTodoList().setSelectedTasks(tasks);
        Intent intent = new Intent(this, AddTask.class);
        startActivity(intent);
    }

    private void noteToSelf(@NonNull Intent intent, @NonNull String append_text) {
        String task = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            log.debug("Voice note added.");
        }
        addBackgroundTask(task, append_text);
    }

    private void addBackgroundTask(@NonNull String sharedText, @NonNull String appendText) {
        TodoList todoList = m_app.getTodoList();
        ArrayList<Task> addedTasks = new ArrayList<>();
        log.debug("Adding background tasks to todolist {} ", todoList);

        for (String taskText : sharedText.split("\r\n|\r|\n")) {
            if (taskText.trim().isEmpty()) {
                continue;
            }
            if (!appendText.isEmpty()) {
                taskText = taskText + " " + appendText;
            }
            Task t;
            if (m_app.hasPrependDate()) {
                t = new Task(taskText, DateTime.today(TimeZone.getDefault()));
            } else {
                t = new Task(taskText);
            }
            todoList.add(t);
            addedTasks.add(t);
        }
        todoList.notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(), m_app, true);
        finish();
        Util.showToastShort(m_app, R.string.task_added);
        if (m_app.hasShareTaskShowsEdit()) {
            startAddTaskActivity(addedTasks);
        }
    }

}
