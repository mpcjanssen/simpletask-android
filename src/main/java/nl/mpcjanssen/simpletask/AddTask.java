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
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskBag;
import nl.mpcjanssen.simpletask.util.Strings;
import nl.mpcjanssen.simpletask.util.Util;
import nl.mpcjanssen.simpletask.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;


public class AddTask extends Activity {

    private final static String TAG = AddTask.class.getSimpleName();

    private Task m_backup;
    private TodoApplication m_app;
    private TaskBag taskBag;

    private String share_text;


    private EditText textInputField;

    public boolean hasCloneTags() {
        return ((CheckBox) findViewById(R.id.cb_clone)).isChecked();
    }

    public void setCloneTags(boolean bool) {
        ((CheckBox) findViewById(R.id.cb_clone)).setChecked(bool);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_task, menu);
        return true;
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
                // save clone checkbox state
                m_app.setAddTagsCloneTags(hasCloneTags());
                // strip line breaks
                textInputField = (EditText) findViewById(R.id.taskText);
                String input = textInputField.getText().toString();
                ArrayList<String> tasks = new ArrayList<String>();
                tasks.addAll(Arrays.asList(input.split("\\r\\n|\\r|\\n")));
                if (m_backup != null && tasks.size()>0) {
                    // When updating take the first line as the updated tasks
                    taskBag.updateTask(m_backup, tasks.get(0));
                    tasks.remove(0);
                }
                // Add any other tasks
                for (String taskText : tasks) {
                    taskBag.addAsTask(taskText);
                }

                m_app.setNeedToPush(true);
                m_app.updateWidgets();
                if (m_app.isAutoArchive()) {
                    taskBag.archive();
                }
                sendBroadcast(new Intent(
                        getPackageName() + Constants.BROADCAST_START_SYNC_WITH_REMOTE));
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

    private void noteToSelf(Intent intent) {
        String task = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (intent.hasExtra(Intent.EXTRA_STREAM)) {
            // This was a voice note
        }
        addBackgroundTask(task);
    }

    private void addBackgroundTask(String taskText) {
        taskBag.addAsTask(taskText);
        m_app.setNeedToPush(true);
        m_app.updateWidgets();
        sendBroadcast(new Intent(
                getPackageName() + Constants.BROADCAST_START_SYNC_WITH_REMOTE));
        m_app.showToast(R.string.task_added);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate()");
        m_app = (TodoApplication) getApplication();
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
        } else if (Constants.INTENT_BACKGROUND_TASK.equals(action)) {
            Log.v(TAG, "Adding background task");
            if (intent.hasExtra(Constants.EXTRA_BACKGROUND_TASK)) {
                addBackgroundTask(intent.getStringExtra(Constants.EXTRA_BACKGROUND_TASK));
            } else {
                Log.w(TAG, "Task was not in extras");
            }
            finish();
            return;
        }

        // Set the proper theme
        setTheme(m_app.getActiveTheme());
        setContentView(R.layout.add_task);

        // text
        textInputField = (EditText) findViewById(R.id.taskText);

        if (share_text != null) {
            textInputField.setText(share_text);
        }

        Task iniTask = null;
        setTitle(R.string.addtask);

        m_backup = (Task) intent.getSerializableExtra(
                Constants.EXTRA_TASK);
        if (m_backup != null) {
            textInputField.setText(m_backup.inFileFormat());
            setTitle(R.string.updatetask);
            textInputField.setSelection(m_backup.inFileFormat().length());
        } else {
            if (textInputField.getText().length() == 0) {
                ArrayList<String> projects = new ArrayList<String>();
                String value = intent.getStringExtra(Constants.INTENT_PROJECTS_FILTER);
                if (!Strings.isEmptyOrNull(value)) {
                    projects.addAll(Arrays.asList(value.split("\n")));
                }
                ArrayList<String> contexts = new ArrayList<String>();
                value = intent.getStringExtra(Constants.INTENT_CONTEXTS_FILTER);
                if (!Strings.isEmptyOrNull(value)) {
                    contexts.addAll(Arrays.asList(value.split("\n")));
                }
                iniTask = new Task(1, "");
                iniTask.initWithFilters(contexts, projects);
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
        }
        // Listen to enter
        textInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    // Move cursor to end of line
                    int position = textInputField.getSelectionStart();
                    String remainingText = textInputField.getText().toString().substring(position);
                    int endOfLineDistance = remainingText.indexOf('\n');
                    int endOfLine;
                    if (endOfLineDistance == -1) {
                        endOfLine = textInputField.length();
                    } else {
                        endOfLine = position + endOfLineDistance;
                    }
                    textInputField.setSelection(endOfLine);

                    if (hasCloneTags()) {
                        String precedingText = textInputField.getText().toString().substring(0, endOfLine);
                        int lineStart = precedingText.lastIndexOf('\n');
                        String line = "";
                        if (lineStart != -1) {
                            line = precedingText.substring(lineStart, endOfLine);
                        } else {
                            line = precedingText;
                        }
                        Task t = new Task(0, line);
                        LinkedHashSet<String> tags = new LinkedHashSet<String>();
                        for (String ctx : t.getContexts()) {
                            tags.add("@" + ctx);
                        }
                        for (String prj : t.getProjects()) {
                            tags.add("+" + prj);
                        }
                        replaceTextAtSelection(Util.join(tags, " "));
                        textInputField.setSelection(endOfLine);
                    }

                }
                return false;
            }
        });
        setCloneTags(m_app.isAddTagsCloneTags());
        int textIndex = 0;
        textInputField.setSelection(textIndex);
    }


    private void showTagMenu(View v) {
        final ArrayList<String> projects = taskBag.getProjects(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(projects.toArray(new String[projects.size()]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int which) {
                        replaceTextAtSelection("+" + projects.get(which) + " ");
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

        builder.setItems(priorityCodes.toArray(new String[priorityCodes.size()]),
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
        final ArrayList<String> contexts = taskBag.getContexts(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(contexts.toArray(new String[contexts.size()]),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int which) {
                        replaceTextAtSelection("@" + contexts.get(which) + " ");
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
        Collections.addAll(lines, textInputField.getText().toString().split("\\n", -1));

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
