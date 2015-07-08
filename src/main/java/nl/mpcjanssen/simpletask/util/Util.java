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
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;
import com.google.common.base.Joiner;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.BackupDbHelper;
import nl.mpcjanssen.simpletask.Constants;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.TodoException;
import nl.mpcjanssen.simpletask.sort.AlphabeticalStringComparator;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TodoList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.luaj.vm2.*;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private static String TAG = Util.class.getSimpleName();

    private Util() {
    }

    public static String getTodayAsString() {
        return DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT);
    }

    public static void runOnMainThread (Runnable r) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(r);
    }


    public static void showToastShort(@NotNull final Context cxt, final int resid) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showToastLong(@NotNull final Context cxt, final int resid) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, resid, Toast.LENGTH_LONG).show();
            }
        });
    }


    public static void showToastShort(@NotNull final Context cxt, final String msg) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showToastLong(@NotNull final Context cxt, final String msg) {
        runOnMainThread (new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    public static List<String> tasksToString(@NotNull List<Task> tasks) {
        ArrayList<String> result = new ArrayList<>();
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
    public static String joinTasks(@Nullable Collection<Task> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (s==null) {
            return "";
        }
        Iterator<Task> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next().inFileFormat());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    @NotNull
    public static String join(@Nullable Collection<String> s, String delimiter) {
        if (s==null) {
            return "";
        }
        return Joiner.on(delimiter).join(s);
    }

    public static void setColor(@NotNull SpannableString ss, int color, String s) {
        ArrayList<String> strList = new ArrayList<>();
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
        switch(type) {
            case "d":
                date = date.plusDays(amount);
                break;
            case "w":
                date = date.plusDays(7 * amount);
                break;
            case "m":
                date = date.plus(0, amount, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
                break;
            case "y":
                date = date.plus(amount, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay);
                break;
            default:
                // Dont add anything
                break;
        }
        return date;
    }

    @NotNull
    public static ArrayList<String> prefixItems(String prefix, @NotNull ArrayList<String> items) {
        ArrayList<String> result = new ArrayList<>();
        for (String item : items) {
            result.add(prefix + item);
        }
        return result;
    }

    @NotNull
    public static ArrayList<String> getCheckedItems(@NotNull ListView listView , boolean checked) {
        SparseBooleanArray checks = listView.getCheckedItemPositions();
        ArrayList<String> items = new ArrayList<>();
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


    public static void initGlobals(Globals globals, Task t) {
        globals.set("task", t.inFileFormat());

        if (t.getDueDate()!=null) {
            globals.set( "due", t.getDueDate().getMilliseconds(TimeZone.getDefault())/1000);
        } else {
            globals.set("due",LuaValue.NIL);
        }


        if (t.getThresholdDate()!=null) {
            globals.set("threshold", t.getThresholdDate().getMilliseconds(TimeZone.getDefault())/1000);
        } else {
            globals.set("threshold",LuaValue.NIL);
        }


        if (t.getCreateDate()!=null) {
            globals.set("createdate", new DateTime(t.getCreateDate()).getMilliseconds(TimeZone.getDefault())/1000);
        } else {
            globals.set("createdate",LuaValue.NIL);
        }


        if (t.getCompletionDate()!=null) {
            globals.set("completiondate", new DateTime(t.getCompletionDate()).getMilliseconds(TimeZone.getDefault())/1000);
        } else {
            globals.set("completiondate",LuaValue.NIL);
        }

        globals.set( "completed", LuaBoolean.valueOf(t.isCompleted()));
        globals.set( "priority", t.getPriority().getCode());

        globals.set("tags", javaListToLuaTable(t.getTags()));
        globals.set("lists", javaListToLuaTable(t.getLists()));
    }

    private static LuaValue javaListToLuaTable(List<String>javaList) {
        int size = javaList.size();
        if (size==0) return LuaValue.NIL;
        LuaString[] luaArray = new LuaString[javaList.size()];
        int i = 0;
        for (String item : javaList) {
            luaArray[i] = LuaString.valueOf(item);
            i++;
        }
        return LuaTable.listOf(luaArray);
    
    }

    public static void createCachedFile(Context context, String fileName,
            String content) throws IOException {

        File cacheFile = new File(context.getCacheDir() + File.separator
                + fileName);
        if (cacheFile.createNewFile()) {
            FileOutputStream fos = new FileOutputStream(cacheFile);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            PrintWriter pw = new PrintWriter(osw);
            pw.println(content);
            pw.flush();
            pw.close();
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    public static void createCachedDatabase(Context context, File dbFile) throws IOException {
        File cacheFile = new File(context.getCacheDir() , "history.db");
        copyFile(dbFile,cacheFile);
    }

    public static ArrayList<String> sortWithPrefix(List<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> result = new ArrayList<>();
        result.addAll(new AlphabeticalStringComparator(caseSensitive).sortedCopy(items));
        if (prefix !=null ) {
            result.add(0, prefix);
        }
        return result;
    }

    public static ArrayList<String> sortWithPrefix(Set<String> items, boolean caseSensitive, String prefix) {
        ArrayList<String> temp = new ArrayList<>();
        temp.addAll(items);
        return sortWithPrefix(temp, caseSensitive, prefix);
    }


    public static Dialog showLoadingOverlay(@NotNull Activity act, @Nullable Dialog visibleDialog, boolean show) {
        if (show) {
            Dialog newDialog = new Dialog(act);
            newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            newDialog.setContentView(R.layout.loading);
            newDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            newDialog.setCancelable(false);
            newDialog.show();
            return newDialog;
        } else if (!show && visibleDialog!=null) {
            visibleDialog.dismiss();
        }
        return null;
    }
}
