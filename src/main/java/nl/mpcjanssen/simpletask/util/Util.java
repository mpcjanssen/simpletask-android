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
package nl.mpcjanssen.simpletask.util;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.widget.ListView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.TodoException;
import nl.mpcjanssen.simpletask.sort.AlphabeticalStringComparator;
import nl.mpcjanssen.simpletask.task.Task;

public class Util {

    private static String TAG = Util.class.getSimpleName();

    private Util() {
    }

    public static String getTodayAsString() {
        return DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT);
    }

    public static void showToastShort(@NotNull Context cxt, int resid) {
        Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
    }

    public static void showToastShort(@NotNull Context cxt, String msg) {
        Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
    }

    @Nullable
    public static ArrayList<String> tasksToString(@NotNull List<Task> tasks) {
        if (tasks==null) {
            return null;
        }
        ArrayList<String> result = new ArrayList<String>();
        for (Task t: tasks) {
            result.add(t.inFileFormat());
        }
        return result;
    }

    public interface InputDialogListener {
        void onClick(String input);
    }

    public static void createParentDirectory(@Nullable File dest) throws TodoException {
        if (dest == null) {
            throw new TodoException("createParentDirectory: dest is null");
        }
        File dir = dest.getParentFile();
        if (dir != null && !dir.exists()) {
            createParentDirectory(dir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, "Could not create dirs: " + dir.getAbsolutePath());
                    throw new TodoException("Could not create dirs: "
                            + dir.getAbsolutePath());
                }
            }
        }
    }

    @NotNull
    public static String join(@Nullable Collection<?> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (s==null) {
        	return "";
        }
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    public static void setColor(@NotNull SpannableString ss, int color, String s) {
        ArrayList<String> strList = new ArrayList<String>();
        strList.add(s);
        setColor(ss,color,strList);
    }

    public static void setColor(@NotNull SpannableString ss, int color ,  @NotNull List<String> items) {
        String data = ss.toString();
        for (String item : items) {
            int i = data.indexOf(item);
            if (i != -1) {
                ss.setSpan(new ForegroundColorSpan(color), i,
                        i + item.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static void setColor(@NotNull SpannableString ss, int color) {

        ss.setSpan(new ForegroundColorSpan(color), 0,
                ss.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Nullable
    public static DateTime addInterval(@Nullable DateTime date, @NotNull String interval) {
        Pattern p = Pattern.compile("(\\d+)([dwmy])");
        Matcher m = p.matcher(interval.toLowerCase(Locale.getDefault()));
        int amount;
        String type;
        if (date == null) {
            date = DateTime.today(TimeZone.getDefault());
        }
        if(!m.find()) {
            return null;
        }
        if(m.groupCount()==2) {
            amount = Integer.parseInt(m.group(1));
            type = m.group(2).toLowerCase(Locale.getDefault());
        } else {
            return null;
        }
        if (type.equals("d")) {
            date = date.plusDays(amount);
        } else if (type.equals("w")) {
            date = date.plusDays(7 * amount);
        } else if (type.equals("m")) {
            date = date.plus(0, amount, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
        } else if (type.equals("y")) {
            date = date.plus(amount, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
        }
        return date;
    }

    @NotNull
    public static ArrayList<String> prefixItems(String prefix, @NotNull ArrayList<String> items) {
        ArrayList<String> result = new ArrayList<String>();
        for (String item : items) {
            result.add(prefix + item);
        }
        return result;
    }

    @NotNull
    public static ArrayList<String> getCheckedItems(@NotNull ListView listView , boolean checked) {
        SparseBooleanArray checks = listView.getCheckedItemPositions();
        ArrayList<String> items = new ArrayList<String>();
        for (int i = 0 ; i < checks.size() ; i++) {
            String item = (String)listView.getAdapter().getItem(checks.keyAt(i));
            if (checks.valueAt(i) && checked) {
                items.add(item);
            } else if (!checks.valueAt(i) && !checked) {
                items.add(item);
            }
        }
        return items;
    }

    @NotNull
    public static AlertDialog createDeferDialog(@NotNull final Activity act, int dateType, final boolean showNone,  @NotNull final InputDialogListener listener) {
        String[] keys = act.getResources().getStringArray(R.array.deferOptions);
        String today = "0d";
        String tomorrow = "1d";
        String oneWeek = "1w";
        String twoWeeks = "2w";
        String oneMonth = "1m";
        final String[] values  = { "", today, tomorrow, oneWeek, twoWeeks, oneMonth, "pick" };
        if (!showNone) {
            keys = Arrays.copyOfRange(keys, 1, keys.length);
        }
        int titleId;
        if (dateType==Task.DUE_DATE) {
            titleId = R.string.defer_due;
        } else {
            titleId = R.string.defer_threshold;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(act);
        builder.setTitle(titleId);
        builder.setItems(keys,  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!showNone) {
                    whichButton++;
                }
                String selected = values[whichButton];
                listener.onClick(selected);
            }
        });
        return builder.create();
    }

    public static void fillScope(ScriptableObject scope, Task t) {
        scope.defineProperty("task", t.inFileFormat(), 0);
        if (t.getDueDate()!=null) {
            scope.defineProperty("due", t.getDueDate().getMilliseconds(TimeZone.getDefault()), 0);
        } else {
            scope.defineProperty("due", t.getDueDate(), 0);
        }
        if (t.getThresholdDate()!=null) {
            scope.defineProperty("threshold", t.getThresholdDate().getMilliseconds(TimeZone.getDefault()), 0);
        } else {
            scope.defineProperty("threshold",t.getThresholdDate(), 0);
        }
        if (t.getCreateDate()!=null) {
            scope.defineProperty("createdate", new DateTime(t.getCreateDate()).getMilliseconds(TimeZone.getDefault()), 0);
        } else {
            scope.defineProperty("createdate",t.getCreateDate(), 0);
        }
        if (t.getCompletionDate()!=null) {
            scope.defineProperty("completiondate", new DateTime(t.getCompletionDate()).getMilliseconds(TimeZone.getDefault()), 0);
        } else {
            scope.defineProperty("completiondate",t.getCompletionDate(), 0);
        }
        scope.defineProperty("completed", t.isCompleted(), 0);
        scope.defineProperty("priority", t.getPriority().getCode(),0);
        scope.defineProperty("recurrence", t.getRecurrencePattern(), 0);
        scope.defineProperty("tags", org.mozilla.javascript.Context.javaToJS(t.getTags(), scope), 0);
        scope.defineProperty("lists", org.mozilla.javascript.Context.javaToJS(t.getLists(), scope), 0);
    }

    public static void createCachedFile(Context context, String fileName,
            String content) throws IOException {
 
        File cacheFile = new File(context.getCacheDir() + File.separator
                + fileName);
        cacheFile.createNewFile();
 
        FileOutputStream fos = new FileOutputStream(cacheFile);
        OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
        PrintWriter pw = new PrintWriter(osw);
 
        pw.println(content);
 
        pw.flush();
        pw.close();
    }

    public static ArrayList<String> sortWithPrefix(List<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> result = new ArrayList<String>();
        result.addAll(new AlphabeticalStringComparator(caseSensitive).sortedCopy(items));
        if (prefix !=null ) {
            result.add(0,prefix);
        }
        return result;
    }

    public static ArrayList<String> sortWithPrefix(Set<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> temp = new ArrayList<String>();
        temp.addAll(items);
        return sortWithPrefix(temp, caseSensitive, prefix);
    }
}
