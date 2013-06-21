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
package nl.mpcjanssen.simpletask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.Layout;
import android.text.Selection;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletaskdonate.R;

import java.util.*;


public class AddTask extends Activity {

    private final static String TAG = AddTask.class.getSimpleName();

    private ProgressDialog m_ProgressDialog = null;

    private Task m_backup;
    private MainApplication m_app;
    private TaskBag taskBag;

    private String share_text;


    private EditText textInputField;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_task, menu);
        return true;
    }

    private void noteToSelf (Intent intent) {
        String task = intent.getStringExtra(Intent.EXTRA_TEXT);
        taskBag.addAsTask(task);
        taskBag.store();
        m_app.updateWidgets();
        Log.v(TAG, "Note to self: " + task);
        m_app.showToast(R.string.task_added);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add_task:
                // Open new tasks add activity
                Intent intent = getIntent();
                intent.removeExtra(Constants.EXTRA_TASK);
                startActivity(intent);
                // And save current task
            case R.id.menu_save_task:
                // strip line breaks
                textInputField = (EditText) findViewById(R.id.taskText);
                String input = textInputField.getText().toString();
                if (m_backup != null) {
                    taskBag.delete(m_backup);
                }

                for (String taskText : input.split("\\r\\n|\\r|\\n")) {
                    taskBag.addAsTask(taskText);
                }
                MainApplication m_app = (MainApplication) getApplication();
                m_app.updateWidgets();
                taskBag.store();
                finish();
                break;
            case R.id.menu_add_task_help:
                Dialog dialog = new Dialog(this);
                dialog.setContentView(R.layout.help);
                dialog.setCancelable(true);
                dialog.setTitle("Task format");
                dialog.show();
                break;
            case R.id.menu_add_prio:
                showPrioMenu(findViewById(R.id.menu_add_prio));
                break;
            case R.id.menu_add_list:
                showContextMenu(findViewById(R.id.menu_add_list));
                break;
            case R.id.menu_add_tag:
                showTagMenu(findViewById(R.id.menu_add_tag));
                break;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");

        m_app = (MainApplication) getApplication();
        taskBag = m_app.getTaskBag();

        final Intent intent = getIntent();
        final String action = intent.getAction();
        // create shortcut and exit
        if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            Log.d(TAG, "Setting up shortcut icon");
            setupShortcut();
            finish();
            return;
        } else if (Intent.ACTION_SEND.equals(action)) {
            Log.d(TAG, "Share");
            share_text = (String) intent
                    .getCharSequenceExtra(Intent.EXTRA_TEXT);
            Log.d(TAG, share_text);
        } else if ("com.google.android.gm.action.AUTO_SEND".equals(action)) {
            // Called as note to self from google search/now
            noteToSelf(intent);
            finish();
            return;
        }


        setContentView(R.layout.add_task);

        // text
        textInputField = (EditText) findViewById(R.id.taskText);

        if (share_text != null) {
            textInputField.setText(share_text);
        }

        Task iniTask = null;
        setTitle(R.string.addtask);

        Task task = (Task) intent.getSerializableExtra(
                Constants.EXTRA_TASK);
        if (task != null) {
            m_backup = task;
            textInputField.setText(task.inFileFormat());
            setTitle(R.string.updatetask);
            textInputField.setSelection(task.inFileFormat().length());
        } else {
            if (textInputField.getText().length() == 0) {
                ArrayList<String> projects = (ArrayList<String>) intent.getSerializableExtra(Constants.EXTRA_PROJECTS_SELECTED);
                ArrayList<String> contexts = (ArrayList<String>) intent.getSerializableExtra(Constants.EXTRA_CONTEXTS_SELECTED);
                iniTask = new Task(1, "");
                iniTask.initWithFilters(contexts, projects);
            }
        }

        if (iniTask != null && iniTask.getProjects().size() == 1) {
            List<String> ps = iniTask.getProjects();
            String project = ps.get(0);
            if (!project.equals("-")) {
                textInputField.append(" +" + project);
            }
        }


        if (iniTask != null && iniTask.getContexts().size() == 1) {
            List<String> cs = iniTask.getContexts();
            String context = cs.get(0);
            if (!context.equals("-")) {
                textInputField.append(" @" + context);
            }
        }

        int textIndex = 0;
        textInputField.setSelection(textIndex);
    }

    private void showTagMenu(View v) {
        // Get projects in taskbag and in task to be added.
        Set<String> allProjects = new HashSet<String>();
        allProjects.addAll(taskBag.getProjects(false));

        // Create single task from all lines to extract projects
        Task t = new Task(1,textInputField.getText().toString().replaceAll("\\n", " "));
        allProjects.addAll(t.getProjects());
        ArrayList<String> projects = new ArrayList<String>(allProjects);
        Collections.sort(projects);

        final ArrayList<String> sortedProjects = projects;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(projects.toArray(new String[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int which) {
                        replaceTextAtSelection("+" + sortedProjects.get(which) + " ");
                    }
                });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.project_prompt);
        dialog.show();
    }

    private void showPrioMenu(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Priority[] priorities = Priority.values();
        ArrayList<String> priorityCodes = new ArrayList<String>();

        for (Priority prio : priorities) {
            priorityCodes.add(prio.getCode());
        }

        builder.setItems(priorityCodes.toArray(new String[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int which) {
                        replacePriority(priorities[which].getCode());
                    }
                });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.priority_prompt);
        dialog.show();
    }


    private void showContextMenu(View v) {
        // Get contexts in taskbag and in task to be added.
        Set<String> allContexts = new HashSet<String>();
        allContexts.addAll(taskBag.getContexts(false));

        // Create single task from all lines to extract contexts
        Task t = new Task(1,textInputField.getText().toString().replaceAll("\\n", " "));
        allContexts.addAll(t.getContexts());
        ArrayList<String> contexts = new ArrayList<String>(allContexts);
        Collections.sort(contexts);

        final ArrayList<String> sortedContexts = contexts;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(contexts.toArray(new String[0]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int which) {
                        replaceTextAtSelection("@" + sortedContexts.get(which) + " ");
                    }
                });

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(R.string.context_prompt);
        dialog.show();
    }

    public int getCurrentCursorLine(EditText editText) {
        int selectionStart = Selection.getSelectionStart(editText.getText());
        Layout layout = editText.getLayout();

        if (selectionStart != -1) {
            return layout.getLineForOffset(selectionStart);
        }

        return -1;
    }

    private void replacePriority(CharSequence newPrio) {
        // save current selection and length
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        int length = textInputField.getText().length();
        int sizeDelta;
        ArrayList<String> lines = new ArrayList<String>();
        for (String line : textInputField.getText().toString().split("\\n", -1)) {
            lines.add(line);
        }
        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        int currentLine = getCurrentCursorLine(textInputField);
        if (currentLine > lines.size() - 1) {
            currentLine = lines.size() - 1;
        }
        if (currentLine != -1) {
            Task t = new Task(0, lines.get(currentLine));
            t.setPriority(Priority.toPriority(newPrio.toString()));
            lines.set(currentLine, t.inFileFormat());
            textInputField.setText(Util.join(lines, "\n"));
        }
        // restore selection
        int newLength = textInputField.getText().length();
        sizeDelta = newLength - length;
        int newStart = Math.max(0, start + sizeDelta);
        int newEnd = Math.min(end + sizeDelta, newLength);
        newEnd = Math.max(newStart, newEnd);
        textInputField.setSelection(newStart, newEnd);

    }

    private void replaceTextAtSelection(CharSequence title) {
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        if (start == end && start != 0) {
            // no selection prefix with space if needed
            if (!(textInputField.getText().charAt(start - 1) == ' ')) {
                title = " " + title;
            }
        }
        textInputField.getText().replace(Math.min(start, end), Math.max(start, end),
                title, 0, title.length());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (m_ProgressDialog != null) {
            m_ProgressDialog.dismiss();
        }
    }

    private void setupShortcut() {
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(this, this.getClass().getName());

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                getString(R.string.shortcut_addtask_name));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this,
                R.drawable.icon);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        setResult(RESULT_OK, intent);
    }
}
