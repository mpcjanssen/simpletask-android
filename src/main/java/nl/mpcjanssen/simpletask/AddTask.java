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

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import nl.mpcjanssen.simpletask.util.InputDialogListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Pattern;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;
import nl.mpcjanssen.simpletask.util.Util;


public class AddTask extends ThemedActivity {

    private TodoApplication m_app;

    private String share_text;

    private EditText textInputField;
    private BroadcastReceiver m_broadcastReceiver;
    private LocalBroadcastManager localBroadcastManager;
    private List<Task> m_backup = new ArrayList<>();
    private Logger log;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        log = LoggerFactory.getLogger(this.getClass());
        log.debug("onCreate()");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        m_app = (TodoApplication) getApplication();

        final Intent intent = getIntent();
        ActiveFilter mFilter = new ActiveFilter();
        mFilter.initFromIntent(intent);



        ActionBar actionBar = getActionBar();
        if (actionBar!=null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.BROADCAST_UPDATE_UI);
        intentFilter.addAction(Constants.BROADCAST_SYNC_START);
        intentFilter.addAction(Constants.BROADCAST_SYNC_DONE);

        localBroadcastManager = m_app.getLocalBroadCastManager();

        m_broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_SYNC_START)) {
                    setProgressBarIndeterminateVisibility(true);
                } else if (intent.getAction().equals(Constants.BROADCAST_SYNC_DONE)) {
                    setProgressBarIndeterminateVisibility(false);
                }
            }
        };
        localBroadcastManager.registerReceiver(m_broadcastReceiver, intentFilter);




        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        setContentView(R.layout.add_task);

        // text
        textInputField = (EditText) findViewById(R.id.taskText);
        m_app.setEditTextHint(textInputField, R.string.tasktexthint);

        if (share_text != null) {
            textInputField.setText(share_text);
        }

        Task iniTask = null;
        setTitle(R.string.addtask);

        TodoList todoList = m_app.getTodoList();

        m_backup = todoList.getSelectedTasks();

        if (m_backup!=null && m_backup.size()>0) {
            ArrayList<String> prefill = new ArrayList<>();
            for (Task t : m_backup) {
                prefill.add(t.inFileFormat());
            }
            String sPrefill = Util.join(prefill,"\n");
            textInputField.setText(sPrefill);
            setTitle(R.string.updatetask);
        } else {
            if (textInputField.getText().length() == 0) {
                iniTask = new Task("");
                iniTask.initWithFilter(mFilter);
            }

            if (iniTask != null && iniTask.getTags().size() == 1) {
                List<String> ps = iniTask.getTags();
                String project = ps.get(0);
                if (!project.equals("-")) {
                    textInputField.append(" +" + project);
                }
            }


            if (iniTask != null && iniTask.getLists().size() == 1) {
                List<String> cs = iniTask.getLists();
                String context = cs.get(0);
                if (!context.equals("-")) {
                    textInputField.append(" @" + context);
                }
            }
        }
        // Listen to enter events, use IME_ACTION_NEXT for soft keyboards
        // like Swype where ENTER keyCode is not generated.

        int inputFlags = InputType.TYPE_CLASS_TEXT;

        if (m_app.hasCapitalizeTasks()) {
            inputFlags |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        }
        textInputField.setRawInputType(inputFlags);
        textInputField.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        textInputField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, @Nullable KeyEvent keyEvent) {

                boolean hardwareEnterUp = keyEvent!=null &&
                        keyEvent.getAction() == KeyEvent.ACTION_UP &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean hardwareEnterDown = keyEvent!=null &&
                        keyEvent.getAction() == KeyEvent.ACTION_DOWN &&
                        keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER;
                boolean imeActionNext = (actionId == EditorInfo.IME_ACTION_NEXT);

                if (imeActionNext || hardwareEnterUp ) {
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
                    replaceTextAtSelection("\n", false);

                    if (hasCloneTags()) {
                        String precedingText = textInputField.getText().toString().substring(0, endOfLine);
                        int lineStart = precedingText.lastIndexOf('\n');
                        String line;
                        if (lineStart != -1) {
                            line = precedingText.substring(lineStart, endOfLine);
                        } else {
                            line = precedingText;
                        }
                        Task t = new Task(line);
                        LinkedHashSet<String> tags = new LinkedHashSet<>();
                        for (String ctx : t.getLists()) {
                            tags.add("@" + ctx);
                        }
                        for (String prj : t.getTags()) {
                            tags.add("+" + prj);
                        }
                        replaceTextAtSelection(Util.join(tags, " "), true);
                    }
                    endOfLine++;
                    textInputField.setSelection(endOfLine);
                }
                return (imeActionNext || hardwareEnterDown || hardwareEnterUp);
            }
        });

        setCloneTags(m_app.isAddTagsCloneTags());
        setWordWrap(m_app.isWordWrap());

        findViewById(R.id.cb_wrap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setWordWrap(hasWordWrap());
            }
        });

        int textIndex = 0;
        textInputField.setSelection(textIndex);

        // Set button callbacks
        findViewById(R.id.btnContext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showContextMenu();
            }
        });
        findViewById(R.id.btnProject).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTagMenu();
            }
        });
        findViewById(R.id.btnPrio).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrioMenu();
            }
        });

        findViewById(R.id.btnDue).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertDate(Task.DUE_DATE);
            }
        });
        findViewById(R.id.btnThreshold).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertDate(Task.THRESHOLD_DATE);
            }
        });

        if (m_backup!=null && m_backup.size()>0) {
            textInputField.setSelection(textInputField.getText().length());
        }
    }

    public boolean hasWordWrap() {
        return ((CheckBox) findViewById(R.id.cb_wrap)).isChecked();
    }

    public void setWordWrap(boolean bool) {
        ((CheckBox) findViewById(R.id.cb_wrap)).setChecked(bool);
        if (textInputField!=null) {
            textInputField.setHorizontallyScrolling(!bool);
        }
    }

    public boolean hasCloneTags() {
        return ((CheckBox) findViewById(R.id.cb_clone)).isChecked();
    }

    public void setCloneTags(boolean bool) {
        ((CheckBox) findViewById(R.id.cb_clone)).setChecked(bool);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
         MenuInflater inflater = getMenuInflater();
         inflater.inflate(R.menu.add_task, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (m_app.isBackSaving()) {
                    saveTasksAndClose();
                }
                Intent upIntent = NavUtils.getParentActivityIntent(this);
                finish();
                startActivity(upIntent);
                return true;
            case R.id.menu_save_task:
                saveTasksAndClose();
                return true;
            case R.id.menu_cancel_task:
                finish();
                return true;
            case R.id.menu_help:
                showHelp();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHelp() {
        Intent i = new Intent(this, HelpScreen.class);
        i.putExtra(Constants.EXTRA_HELP_PAGE,getText(R.string.help_add_task));
        startActivity(i);
    }


    private void saveTasksAndClose() {
        // save clone CheckBox state
        m_app.setAddTagsCloneTags(hasCloneTags());
        m_app.setWordWrap(hasWordWrap());
        
        // strip line breaks
        textInputField = (EditText) findViewById(R.id.taskText);
        String input;
                if (textInputField!=null) {
                    input = textInputField.getText().toString();
                } else {
                    input = "";
                }

        // Don't add empty tasks
        if (input.trim().isEmpty()) {
             finish();
             return;
        }

        // Update the TodoList with changes
        TodoList todoList = m_app.getTodoList();
        ArrayList<Task> updated = new ArrayList<>();
        // Create new tasks
        for (String line : Arrays.asList(input.split("\r\n|\r|\n"))) {

            if(m_app.hasPrependDate()) {
                updated.add(new Task(line,DateTime.now(TimeZone.getDefault())));
            } else {
                updated.add(new Task(line));
            }
        }

        // Update tasks that where selected for update
        if (m_backup!=null){
            for (Task t : m_backup) {
                if (updated.size()>0) {
                    todoList.replace(t,updated.get(0));
                    updated.remove(0);
                } else {
                    todoList.remove(t);
                }
            }
        }
        for (Task rest: updated) {
            todoList.add(rest, m_app.hasAppendAtEnd());
        }

        // Save
        todoList.notifyChanged(m_app.getFileStore(), m_app.getTodoFileName(), m_app.getEol(),m_app, true);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (m_app.isBackSaving()) {
            saveTasksAndClose();
        }
        super.onBackPressed();
    }

    private void insertDate(final int dateType) {
        Dialog d = Util.createDeferDialog(this, dateType, false, new InputDialogListener() {
            @Override
            public void onClick(@NonNull String selected) {
                if (selected.equals("pick")) {
                    /* Note on some Android versions the OnDateSetListener can fire twice
                     * https://code.google.com/p/android/issues/detail?id=34860
                     * With the current implementation which replaces the dates this is not an
                     * issue. The date is just replaced twice
                     */
                    final DateTime today = DateTime.today(TimeZone.getDefault());
                    DatePickerDialog dialog = new DatePickerDialog(AddTask.this, new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                            month++;

                            DateTime date = DateTime.forDateOnly(year,month,day);
                            insertDateAtSelection(dateType, date);
                        }
                    },
                            today.getYear(),
                            today.getMonth()-1,
                            today.getDay());

                    boolean showCalendar = m_app.showCalendar();

                    dialog.getDatePicker().setCalendarViewShown(showCalendar);
                    dialog.getDatePicker().setSpinnersShown(!showCalendar);
                    dialog.show();
                } else {
                    insertDateAtSelection(dateType, Util.addInterval(DateTime.today(TimeZone.getDefault()), selected));
                }
            }
        });
        d.show();
    }

    private void replaceDate(int dateType, @NonNull String date) {
           if (dateType==Task.DUE_DATE) {
               replaceDueDate(date);
           } else {
               replaceThresholdDate(date);
           }
    }

    private void insertDateAtSelection(int dateType, @NonNull DateTime date) {
        replaceDate(dateType, date.format("YYYY-MM-DD"));
    }

    private void showTagMenu() {
        Set<String> items = new TreeSet<>();
        TodoList todoList = m_app.getTodoList();

        items.addAll(todoList.getProjects());
        // Also display contexts in tasks being added
        final Task task = new Task(textInputField.getText().toString());
        items.addAll(task.getTags());
        final ArrayList<String> projects = Util.sortWithPrefix(items, m_app.sortCaseSensitive(),null);


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.tag_dialog, null, false);
        builder.setView(view);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        final EditText ed = (EditText) view.findViewById(R.id.editText);
        ArrayAdapter<String> lvAdapter = new ArrayAdapter<>(this, R.layout.simple_list_item_multiple_choice,
                projects.toArray(new String[projects.size()]));
        lv.setAdapter(lvAdapter);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        initListViewSelection(lv, lvAdapter, task.getTags());

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> items = new ArrayList<>();
                items.addAll(Util.getCheckedItems(lv, true));
                String newText = ed.getText().toString();
                if (!newText.isEmpty()) {
                    items.add(ed.getText().toString());
                }
                for (String item : items) {
                    if (!task.getTags().contains(item)) {
                        replaceTextAtSelection("+" + item + " ", true);
                    }
                }
                for (String taskTagItem : task.getTags()) {
                    if (!items.contains(taskTagItem)) {
                        removeText("+" + taskTagItem);
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(m_app.getTagTerm());
        dialog.show();
    }

    private void showPrioMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Priority[] priorities = Priority.values();
        ArrayList<String> priorityCodes = new ArrayList<>();

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


    private void showContextMenu() {
        Set<String> items = new TreeSet<>();
        TodoList todoList = m_app.getTodoList();

        items.addAll(todoList.getContexts());
        // Also display contexts in tasks being added
        final Task task = new Task(textInputField.getText().toString());
        items.addAll(task.getLists());
        final ArrayList<String> contexts = Util.sortWithPrefix(items, m_app.sortCaseSensitive(),null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        @SuppressLint("InflateParams") View view = getLayoutInflater().inflate(R.layout.tag_dialog, null, false);
        builder.setView(view);
        final ListView lv = (ListView) view.findViewById(R.id.listView);
        final EditText ed = (EditText) view.findViewById(R.id.editText);
        String [] choices = contexts.toArray(new String[contexts.size()]);
        final ArrayAdapter<String> lvAdapter = new ArrayAdapter<>(this, R.layout.simple_list_item_multiple_choice,
                choices);
        lv.setAdapter(lvAdapter);
        lv.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

        initListViewSelection(lv, lvAdapter, task.getLists());

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<String> items = new ArrayList<>();
                items.addAll(Util.getCheckedItems(lv, true));
                String newText = ed.getText().toString();
                if (!newText.isEmpty()) {
                    items.add(ed.getText().toString());
                }
                for (String item : items) {
                    if (!task.getLists().contains(item)) {
                        replaceTextAtSelection("@" + item + " ", true);
                    }
                }
                for (String taskListItem : task.getLists()) {
                    if (!items.contains(taskListItem)) {
                        removeText("@" + taskListItem);
                    }
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.setTitle(m_app.getListTerm());
        dialog.show();
    }

    private void removeText(String text) {
        final String escapedText = Pattern.quote(text);
        final String regexp = "(?:^|\\s)" + escapedText + "(?:\\s|$)";
        final String taskText = String.valueOf(textInputField.getText());
        String newText = taskText.replaceAll(regexp, " ");
        textInputField.setText(newText);
    }

    private void initListViewSelection(ListView lv, ArrayAdapter<String> lvAdapter, List<String> selectedItems) {
        for (int i = 0; i < lvAdapter.getCount(); i++) {
            for (String item : selectedItems) {
                if (item.equals(lvAdapter.getItem(i))) {
                    lv.setItemChecked(i, true);
                }
            }
        }
    }

    public int getCurrentCursorLine(@NonNull EditText editText) {
        int selectionStart = Selection.getSelectionStart(editText.getText());
        Layout layout = editText.getLayout();

        if (selectionStart != -1) {
            return layout.getLineForOffset(selectionStart);
        }

        return -1;
    }

    private void replaceDueDate(@NonNull CharSequence newDueDate) {
        // save current selection and length
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        int length = textInputField.getText().length();
        int sizeDelta;
        ArrayList<String> lines = new ArrayList<>();
        Collections.addAll(lines, textInputField.getText().toString().split("\\n", -1));

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        int currentLine = getCurrentCursorLine(textInputField);
        if (currentLine > lines.size() - 1) {
            currentLine = lines.size() - 1;
        }
        if (currentLine != -1) {
            Task t = new Task(lines.get(currentLine));
            t.setDueDate(newDueDate.toString());
            lines.set(currentLine, t.inFileFormat());
            textInputField.setText(Util.join(lines, "\n"));
        }
    }

    private void replaceThresholdDate(@NonNull CharSequence newThresholdDate) {
        // save current selection and length
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        int length = textInputField.getText().length();
        int sizeDelta;
        ArrayList<String> lines = new ArrayList<>();
        Collections.addAll(lines, textInputField.getText().toString().split("\\n", -1));

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        int currentLine = getCurrentCursorLine(textInputField);
        if (currentLine > lines.size() - 1) {
            currentLine = lines.size() - 1;
        }
        if (currentLine != -1) {
            Task t = new Task(lines.get(currentLine));
            t.setThresholdDate(newThresholdDate.toString());
            lines.set(currentLine, t.inFileFormat());
            textInputField.setText(Util.join(lines, "\n"));
        }
    }

    private void replacePriority(@NonNull CharSequence newPrio) {
        // save current selection and length
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        log.debug("Current selection: " + start + "-" + end);
        int length = textInputField.getText().length();
        int sizeDelta;
        ArrayList<String> lines = new ArrayList<>();
        Collections.addAll(lines, textInputField.getText().toString().split("\\n", -1));

        // For some reason the currentLine can be larger than the amount of lines in the EditText
        // Check for this case to prevent any array index out of bounds errors
        int currentLine = getCurrentCursorLine(textInputField);
        if (currentLine > lines.size() - 1) {
            currentLine = lines.size() - 1;
        }
        if (currentLine != -1) {
            Task t = new Task(lines.get(currentLine));
            log.debug("Changing prio from " + t.getPriority().toString() + " to " + newPrio.toString());
            t.setPriority(Priority.toPriority(newPrio.toString()));
            lines.set(currentLine, t.inFileFormat());
            textInputField.setText(Util.join(lines, "\n"));
        }
    }

    private void replaceTextAtSelection(CharSequence title, boolean spaces) {
        int start = textInputField.getSelectionStart();
        int end = textInputField.getSelectionEnd();
        if (start == end && start != 0 && spaces) {
            // no selection prefix with space if needed
            if (!(textInputField.getText().charAt(start - 1) == ' ')) {
                title = " " + title;
            }
        }
        textInputField.getText().replace(Math.min(start, end), Math.max(start, end),
                title, 0, title.length());
    }

    public void onDestroy() {
        super.onDestroy();
        if (localBroadcastManager!=null) {
            localBroadcastManager.unregisterReceiver(m_broadcastReceiver);
        }
    }
}
