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
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.*;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.Layout;
import android.text.Selection;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Priority;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;
import nl.mpcjanssen.simpletask.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class AddTaskShortcut extends ThemedActivity {

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
        super.onCreate(savedInstanceState);
        m_app = (TodoApplication) getApplication();
        setupShortcut();
        finish();
    }

    private void setupShortcut() {
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setClassName(this, AddTask.class.getName());

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                getString(R.string.shortcut_addtask_name));
        Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this,
                R.drawable.ic_launcher);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

        setResult(Activity.RESULT_OK, intent);
    }
}
