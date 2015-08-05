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


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.SparseBooleanArray;
import android.view.Window;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.common.base.Joiner;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.*;
import nl.mpcjanssen.simpletask.sort.AlphabeticalStringComparator;
import nl.mpcjanssen.simpletask.task.Task;
import org.jetbrains.annotations.NotNull;
import org.luaj.vm2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private Util() {
    }

    public static String getTodayAsString() {
        return DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT);
    }

    public static void runOnMainThread (Runnable r) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(r);
    }


    public static void showToastShort(@NonNull final Context cxt, final int resid) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showToastLong(@NonNull final Context cxt, final int resid) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, resid, Toast.LENGTH_LONG).show();
            }
        });
    }


    public static void showToastShort(@NonNull final Context cxt, final String msg) {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showToastLong(@NonNull final Context cxt, final String msg) {
        runOnMainThread (new Runnable() {
            @Override
            public void run() {
                Toast.makeText(cxt, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    public static List<String> tasksToString(@NonNull List<Task> tasks) {
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
        Logger log = LoggerFactory.getLogger(Util.class);
        if (dest == null) {
            throw new TodoException("createParentDirectory: dest is null");
        }
        File dir = dest.getParentFile();
        if (dir != null && !dir.exists()) {
            createParentDirectory(dir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.error("Could not create dirs: " + dir.getAbsolutePath());
                    throw new TodoException("Could not create dirs: "
                            + dir.getAbsolutePath());
                }
            }
        }
    }

    public static List<VisibleLine> addHeaderLines(List<Task> visibleTasks, String firstSort, String no_header, boolean showHidden, boolean showEmptyLists) {
        String header = "" ;
        String newHeader;
        ArrayList<VisibleLine> result = new ArrayList<>();
        for (Task t : visibleTasks) {
            newHeader = t.getHeader(firstSort, no_header);
            if (!header.equals(newHeader)) {
                VisibleLine headerLine = new VisibleLine(newHeader);
                int last = result.size() - 1;
                if (last != -1 && result.get(last).header && !showEmptyLists) {
                    result.set(last, headerLine);
                } else {
                    result.add(headerLine);
                }
                header = newHeader;
            }

            if (t.isVisible() || showHidden) {
                // enduring tasks should not be displayed
                VisibleLine taskLine = new VisibleLine(t);
                result.add(taskLine);
            }
        }
        return result;
    }

    @NonNull
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

    @NonNull
    public static String join(@Nullable Collection<String> s, String delimiter) {
        if (s==null) {
            return "";
        }
        return Joiner.on(delimiter).join(s);
    }

    public static void setColor(@NonNull SpannableString ss, int color, String s) {
        ArrayList<String> strList = new ArrayList<>();
        strList.add(s);
        setColor(ss,color,strList);
    }

    public static void setColor(@NonNull SpannableString ss, int color ,  @NonNull List<String> items) {
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

    public static void setColor(@NonNull SpannableString ss, int color) {

        ss.setSpan(new ForegroundColorSpan(color), 0,
                ss.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @NotNull
    public static DateTime addInterval(@Nullable DateTime date, @NonNull String interval) {
        Pattern p = Pattern.compile("(\\d+)([dwmy])");
        Matcher m = p.matcher(interval.toLowerCase(Locale.getDefault()));
        int amount;
        String type;
        if (date == null) {
            date = DateTime.today(TimeZone.getDefault());
        }
        if(!m.find()) {
            //If the interval is invalid, just return the original date
            return date;
        }
        if(m.groupCount()==2) {
            amount = Integer.parseInt(m.group(1));
            type = m.group(2).toLowerCase(Locale.getDefault());
        } else {
            return date;
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

    @NonNull
    public static ArrayList<String> prefixItems(String prefix, @NonNull ArrayList<String> items) {
        ArrayList<String> result = new ArrayList<>();
        for (String item : items) {
            result.add(prefix + item);
        }
        return result;
    }

    @NonNull
    public static ArrayList<String> getCheckedItems(@NonNull ListView listView , boolean checked) {
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

    @NonNull
    public static AlertDialog createDeferDialog(@NonNull final Activity act, int dateType, final boolean showNone,  @NonNull final InputDialogListener listener) {
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
        builder.setItems(keys, new DialogInterface.OnClickListener() {
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
            FileOutputStream fos = new FileOutputStream(cacheFile, false);
            OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
            PrintWriter pw = new PrintWriter(osw);
            pw.println(content);
            pw.flush();
            pw.close();
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        Logger log = LoggerFactory.getLogger(Util.class);
        if (destFile.createNewFile()) {
            log.debug("Destination file created {}" , destFile.getAbsolutePath());
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
        File cacheFile = new File(context.getCacheDir() , dbFile.getName());
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

    public static void shareText(Activity act, String text) {
        Logger log = LoggerFactory.getLogger(Util.class);
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Simpletask list");

        // If text is small enough SEND it directly
        if (text.length() < 50000) {
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
        } else {

            // Create a cache file to pass in EXTRA_STREAM
            try {
                Util.createCachedFile(act,
                        Constants.SHARE_FILE_NAME, text);
                Uri fileUri = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/"
                        + Constants.SHARE_FILE_NAME);
                shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri);
            } catch (Exception e) {
                log.warn("Failed to create file for sharing");
            }
        }
        act.startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    public static Dialog showLoadingOverlay(@NonNull Activity act, @Nullable Dialog visibleDialog, boolean show) {
        if (show) {
            Dialog newDialog = new Dialog(act);
            newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            newDialog.setContentView(R.layout.loading);
            ProgressBar pr = (ProgressBar) newDialog.findViewById(R.id.progress);
            pr.getIndeterminateDrawable().setColorFilter(0xFF0099CC, android.graphics.PorterDuff.Mode.MULTIPLY);
            newDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            newDialog.setCancelable(false);
            newDialog.show();
            return newDialog;
        } else if (visibleDialog!=null && visibleDialog.isShowing()) {
            visibleDialog.dismiss();
        }
        return null;
    }
}
