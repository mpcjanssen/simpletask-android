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

import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import nl.mpcjanssen.todotxtholo.task.Priority;
import nl.mpcjanssen.todotxtholo.task.Task;
import nl.mpcjanssen.todotxtholo.task.TaskBag;

import java.util.ArrayList;
import java.util.List;


public class AddTask extends Activity {

    private final static String TAG = AddTask.class.getSimpleName();

    private ProgressDialog m_ProgressDialog = null;

    private Task m_backup;

    private TaskBag taskBag;

    private String share_text;


    private EditText textInputField;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_task, menu);
        boolean edit = getIntent().getBooleanExtra(Constants.EXTRA_EDIT, true);
        if(!edit) {
            for (int i=0 ; i< menu.size() ; i++) {
                menu.getItem(i).setVisible(false);
            }
            menu.findItem(R.id.menu_edit).setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_edit:
                Intent originalIntent = getIntent();
                originalIntent.putExtra(Constants.EXTRA_EDIT,true);
                startActivity(originalIntent);
                finish();
                break;
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
                    // When updating we can only have one line
                    input = input.replaceAll("\\r\\n|\\r|\\n", " ");
                    taskBag.updateTask(m_backup, input);
                } else {
                    for (String taskText : input.split("\\r\\n|\\r|\\n")) {
                        taskBag.addAsTask(taskText);
                    }
                }
                TodoApplication m_app = (TodoApplication) getApplication();
                m_app.updateWidgets();
                if (m_app.isAutoArchive()) {
                    taskBag.archive();
                }
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
        }



        setContentView(R.layout.add_task);


        TodoApplication m_app = (TodoApplication) getApplication();
        taskBag = m_app.getTaskBag();

        // text
        textInputField = (EditText) findViewById(R.id.taskText);

        if (share_text != null) {
            textInputField.setText(share_text);
        }

        Task iniTask = null;
        setTitle(R.string.addtask);

        boolean edit = intent.getBooleanExtra(Constants.EXTRA_EDIT, true);

        Task task = (Task) intent.getSerializableExtra(
                Constants.EXTRA_TASK);
        if (task != null) {
            m_backup = taskBag.find(task);
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


            textInputField.append(" +" + ps.get(0));

        }


        if (iniTask != null && iniTask.getContexts().size() == 1) {
            List<String> cs = iniTask.getContexts();


            textInputField.append(" @" + cs.get(0));

        }

        int textIndex = 0;
        textInputField.setSelection(textIndex);
        if(!edit) {
            setTitle(R.string.viewtask);
            textInputField.setVisibility(View.GONE);
            TextView textView = (TextView)findViewById(R.id.taskView);
            textView.setText(textInputField.getText());
            textView.setVisibility(View.VISIBLE);
            textView.requestFocus();
        }
    }

    private void showTagMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(getApplicationContext(), v);
        Menu menu = popupMenu.getMenu();
        for (String prj : taskBag.getProjects()) {
            menu.add(prj);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                replaceTextAtSelection("+" + item.getTitle() + " ");
                return true;  //To change body of implemented methods use File | Settings | File Templates.
            }
        }

        );
        popupMenu.show();
    }

    private void showPrioMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(getApplicationContext(), v);
        Menu menu = popupMenu.getMenu();
        for (Priority prio : Priority.values()) {
            menu.add(prio.getCode());
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                replacePriority(item.getTitle());
                return true;
            }
        }

        );
        popupMenu.show();
    }

    private void showContextMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(getApplicationContext(), v);
        Menu menu = popupMenu.getMenu();
        for (String ctx : taskBag.getContexts()) {
            menu.add(ctx);
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                replaceTextAtSelection("@" + item.getTitle() + " ");
                return true;  //To change body of implemented methods use File | Settings | File Templates.
            }
        }

        );
        popupMenu.show();
    }

    private void replacePriority(CharSequence newPrio) {
        // save current selection and length
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        int length = textInputField.getText().length();
        int sizeDelta;

        Task t = new Task(0, textInputField.getText().toString());
        t.setPriority(Priority.toPriority(newPrio.toString()));
        textInputField.setText(t.inFileFormat());

        // restore selection
        sizeDelta = textInputField.getText().length() - length;
        textInputField.setSelection(start + sizeDelta, end + sizeDelta);

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
