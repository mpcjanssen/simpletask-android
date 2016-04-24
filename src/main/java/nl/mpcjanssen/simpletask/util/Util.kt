/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).

 * Copyright (c) 2009-2012 Todo.txt contributors (http://todotxt.com)

 * LICENSE:

 * Todo.txt Touch is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Todo.txt Touch is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Todo.txt Touch.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Todo.txt contributors @yahoogroups.com>
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2009-2012 Todo.txt contributors (http://todotxt.com)
 */
@file:JvmName("Util")

package nl.mpcjanssen.simpletask.util


import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.support.v7.app.AlertDialog
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Window
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.*
import nl.mpcjanssen.simpletask.sort.AlphabeticalStringComparator
import nl.mpcjanssen.simpletask.task.TToken
import nl.mpcjanssen.simpletask.task.Task
import nl.mpcjanssen.simpletask.task.TodoListItem

import tcl.lang.*
import java.io.*
import java.nio.channels.FileChannel
import java.util.*
import java.util.regex.Pattern

val TAG = "Util"
val log = Logger;
val todayAsString: String
    get() = DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT)

fun runOnMainThread(r: Runnable) {
    val handler = Handler(Looper.getMainLooper())
    handler.post(r)
}


fun showToastShort(cxt: Context, resid: Int) {
    runOnMainThread(Runnable { Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show() })
}

fun showToastLong(cxt: Context, resid: Int) {
    runOnMainThread(Runnable { Toast.makeText(cxt, resid, Toast.LENGTH_LONG).show() })
}


fun showToastShort(cxt: Context, msg: String) {
    runOnMainThread(Runnable { Toast.makeText(cxt, msg, Toast.LENGTH_SHORT).show() })
}

fun showToastLong(cxt: Context, msg: String) {
    runOnMainThread(Runnable { Toast.makeText(cxt, msg, Toast.LENGTH_LONG).show() })
}

interface InputDialogListener {
    fun onClick(input: String)
}

@Throws(TodoException::class)
fun createParentDirectory(dest: File?) {
    val log = Logger;
    if (dest == null) {
        throw TodoException("createParentDirectory: dest is null")
    }
    val dir = dest.parentFile
    if (dir != null && !dir.exists()) {
        createParentDirectory(dir)
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                log.error(TAG, "Could not create dirs: " + dir.absolutePath)
                throw TodoException("Could not create dirs: " + dir.absolutePath)
            }
        }
    }
}

fun addHeaderLines(visibleTasks: List<TodoListItem>, firstSort: String, no_header: String): List<VisibleLine> {
    var header = ""
    var newHeader: String
    val result = ArrayList<VisibleLine>()
    var count = 0
    var headerLine: HeaderLine? = null
    for (item in visibleTasks) {
        val t = item.task
        newHeader = t.getHeader(firstSort, no_header)
        if (header != newHeader) {
            if (headerLine != null) {
                headerLine.title += " ($count)"
            }
            headerLine = HeaderLine(newHeader)
            count = 0;
            result.add(headerLine)
            header = newHeader
        }
        count++
        val taskLine = TaskLine(item)
        result.add(taskLine)
    }
    // Add count to last header
    if (headerLine != null) {
        headerLine.title += " ($count)"
    }

    // Clean up possible last empty list header that should be hidden
    val i = result.size
    if (i > 0 && result[i - 1].header) {
        result.removeAt(i - 1)
    }
    return result
}

fun join(s: Collection<String>?, delimiter: String): String {
    if (s == null) {
        return ""
    }
    return s.joinToString(delimiter)
}

fun setColor(ss: SpannableString, color: Int, s: String) {
    val strList = ArrayList<String>()
    strList.add(s)
    setColor(ss, color, strList)
}

fun setColor(ss: SpannableString, color: Int, items: List<String>) {
    val data = ss.toString()
    for (item in items) {
        val i = data.indexOf(item)
        if (i != -1) {
            ss.setSpan(ForegroundColorSpan(color), i,
                    i + item.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}

fun setColor(ss: SpannableString, color: Int) {

    ss.setSpan(ForegroundColorSpan(color), 0,
            ss.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
}

fun addInterval(dateTimeStr: String?, interval: String): DateTime? {
    return addInterval(dateTimeStr?.toDateTime(), interval)
}

fun addBusinessDays(originalDate : DateTime, days: Int): DateTime {
    var date = originalDate
    var amount = days
    while (amount > 0) {
        when (date.weekDay) {
            6 -> { // Friday
                date = date.plusDays(3)
            }
            7 -> { // Saturdau
                date = date.plusDays(2)
            }
            else -> {
                date = date.plusDays(1)
            }

        }
        amount -= 1
    }
    return date
}

fun addInterval(date: DateTime?, interval: String): DateTime? {
    var newDate = date
    val p = Pattern.compile("(\\d+)([dDwWmMyYbB])")
    val m = p.matcher(interval.toLowerCase(Locale.getDefault()))
    val amount: Int
    val type: String
    if (newDate == null) {
        newDate = DateTime.today(TimeZone.getDefault())
    }
    if (!m.find()) {
        //If the interval is invalid, just return the original date
        return newDate
    }
    if (m.groupCount() == 2) {
        amount = Integer.parseInt(m.group(1))
        type = m.group(2).toLowerCase(Locale.getDefault())
    } else {
        return newDate
    }
    when (type) {
        "d" -> newDate = newDate!!.plusDays(amount)
        "w" -> newDate = newDate!!.plusDays(7 * amount)
        "m" -> newDate = newDate!!.plus(0, amount, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
        "y" -> newDate = newDate!!.plus(amount, 0, 0, 0, 0, 0, 0, DateTime.DayOverflow.LastDay)
        "b" -> newDate = addBusinessDays(newDate!!, amount)
        else -> {
        }
    }// Dont add anything
    return newDate
}

fun prefixItems(prefix: String, items: ArrayList<String>): ArrayList<String> {
    val result = ArrayList<String>()
    for (item in items) {
        result.add(prefix + item)
    }
    return result
}

fun getCheckedItems(listView: ListView, checked: Boolean): ArrayList<String> {
    val checks = listView.checkedItemPositions
    val items = ArrayList<String>()
    for (i in 0..checks.size() - 1) {
        val item = listView.adapter.getItem(checks.keyAt(i)) as String
        if (checks.valueAt(i) && checked) {
            items.add(item)
        } else if (!checks.valueAt(i) && !checked) {
            items.add(item)
        }
    }
    return items
}

fun createDeferDialog(act: Activity, titleId: Int,  listener: InputDialogListener): AlertDialog {
    var keys = act.resources.getStringArray(R.array.deferOptions)
    val today = "0d"
    val tomorrow = "1d"
    val oneWeek = "1w"
    val twoWeeks = "2w"
    val oneMonth = "1m"
    val values = arrayOf("", today, tomorrow, oneWeek, twoWeeks, oneMonth, "pick")

    val builder = AlertDialog.Builder(act)
    builder.setTitle(titleId)
    builder.setItems(keys) { dialog, whichButton ->
        var which = whichButton
        val selected = values[which]
        listener.onClick(selected)
    }
    return builder.create()
}

fun buildFilterTclCommand(interp: Interp , t: Task) : TclObject {
    val cmd = TclList.newInstance()
    TclList.append(interp, cmd, TclString.newInstance(Constants.TCL_FILTER_COMMAND))
    TclList.append(interp, cmd, TclString.newInstance(t.text))
    val listsObj = TclList.newInstance()
    t.lists.forEach {
        TclList.append(interp,listsObj, TclString.newInstance(it))
    }
    TclList.append(interp, cmd, listsObj)
    return cmd
}

fun buildDisplayTclCommand(interp: Interp , t: Task) : TclObject {
    val cmd = TclList.newInstance()
    TclList.append(interp, cmd, TclString.newInstance(Constants.TCL_DISPLAY_COMMAND))
    TclList.append(interp, cmd, TclString.newInstance(t.text))
    val tokensListObj = TclList.newInstance()

    t.tokens.forEach {
        val tokensObj = TclList.newInstance()
        TclList.append(interp, tokensObj, TclString.newInstance(it.typeAsString()))
        TclList.append(interp, tokensObj, TclString.newInstance(it.value))
        TclList.append(interp, tokensObj, TclString.newInstance(it.text))
        TclList.append(interp, tokensListObj, tokensObj)
    }
    TclList.append(interp, cmd, tokensListObj)
    return cmd
}

fun initGlobals(i: Interp, t: Task) {
    i.setVar("task", t.inFileFormat(), TCL.GLOBAL_ONLY)

    val listsObj = TclList.newInstance()
    t.lists.forEach {
        TclList.append(i,listsObj, TclString.newInstance(it))
    }
    i.setVar("lists", listsObj, TCL.GLOBAL_ONLY)
    val tagsObj = TclList.newInstance()
    t.tags.forEach {
        TclList.append(i,tagsObj, TclString.newInstance(it))
    }
    i.setVar("tags", tagsObj, TCL.GLOBAL_ONLY)

    i.setVar("due", t.dueDate, TCL.GLOBAL_ONLY)
    i.setVar("threshold", t.thresholdDate, TCL.GLOBAL_ONLY)
    i.setVar("createdate", t.createDate, TCL.GLOBAL_ONLY)
    i.setVar("completiondate", t.completionDate, TCL.GLOBAL_ONLY)
    i.setVar("recurrence", t.recurrencePattern?:"", TCL.GLOBAL_ONLY)
    i.setVar("priority", t.priority.code, TCL.GLOBAL_ONLY)
    i.setVar("completed", if (t.isCompleted()) "true" else "false" , TCL.GLOBAL_ONLY)
    i.setVar("today", todayAsString, TCL.GLOBAL_ONLY)
}

@Throws(IOException::class)
fun createCachedFile(context: Context, fileName: String,
                     content: String) {

    val cacheFile = File(context.cacheDir, fileName)
    if (cacheFile.createNewFile()) {
        val fos = FileOutputStream(cacheFile, false)
        val osw = OutputStreamWriter(fos, "UTF8")
        val pw = PrintWriter(osw)
        pw.println(content)
        pw.flush()
        pw.close()
    }
}

@Throws(IOException::class)
fun copyFile(sourceFile: File, destFile: File) {

    if (destFile.createNewFile()) {
        log.debug(TAG, "Destination file created {}" + destFile.absolutePath)
    }

    var source: FileChannel? = null
    var destination: FileChannel? = null

    try {
        source = FileInputStream(sourceFile).channel
        destination = FileOutputStream(destFile).channel
        destination!!.transferFrom(source, 0, source!!.size())
    } finally {
        if (source != null) {
            source.close()
        }
        if (destination != null) {
            destination.close()
        }
    }
}

@Throws(IOException::class)
fun createCachedDatabase(context: Context, dbFile: File) {
    val cacheFile = File(context.cacheDir, dbFile.name)
    copyFile(dbFile, cacheFile)
}

fun sortWithPrefix(items: List<String>, caseSensitive: Boolean, prefix: String?): ArrayList<String> {
    val result = ArrayList<String>()
    result.addAll(items)
    Collections.sort(result, AlphabeticalStringComparator(caseSensitive))
    if (prefix != null) {
        result.add(0, prefix)
    }
    return result
}

fun sortWithPrefix(items: Set<String>, caseSensitive: Boolean, prefix: String?): ArrayList<String> {
    val temp = ArrayList<String>()
    temp.addAll(items)
    return sortWithPrefix(temp, caseSensitive, prefix)
}

fun appVersion (ctx: Context) :String {
    val packageInfo = ctx.packageManager.getPackageInfo(
            ctx.packageName, 0)
    return "Simpletask " + BuildConfig.FLAVOR + " v" + packageInfo.versionName + " (" + BuildConfig.VERSION_CODE + ")"
}

fun shareText(act: Activity, subject: String,  text: String) {

    val shareIntent = Intent(android.content.Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
            subject)

    // If text is small enough SEND it directly
    if (text.length < 50000) {
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text)
    } else {

        // Create a cache file to pass in EXTRA_STREAM
        try {
            createCachedFile(act,
                    Constants.SHARE_FILE_NAME, text)
            val fileUri = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/" + Constants.SHARE_FILE_NAME)
            shareIntent.putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
        } catch (e: Exception) {
            log.warn(TAG, "Failed to create file for sharing")
        }

    }
    act.startActivity(Intent.createChooser(shareIntent, "Share"))
}

fun showLoadingOverlay(act: Activity, visibleDialog: Dialog?, show: Boolean): Dialog? {
    if (show) {
        val newDialog = Dialog(act)
        newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        newDialog.setContentView(R.layout.loading)
        val pr = newDialog.findViewById(R.id.progress) as ProgressBar
        pr.indeterminateDrawable.setColorFilter(-16737844, android.graphics.PorterDuff.Mode.MULTIPLY)
        newDialog.window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        newDialog.setCancelable(false)
        newDialog.show()
        return newDialog
    } else if (visibleDialog != null && visibleDialog.isShowing) {
        visibleDialog.dismiss()
    }
    return null
}

fun getRelativeThresholdDate(task: Task, ctx: Context): String? {
    val date = task.thresholdDate
    if (date != null) {
        date.toDateTime()?.let {
            return "T: " + RelativeDate.getRelativeDate(ctx, it);
        }
    }
    return null;
}

fun getRelativeDueDate(task: Task, application : TodoApplication, dueTodayColor: Int, overDueColor: Int, useColor: Boolean): SpannableString? {
    val date = task.dueDate
    if (date != null) {
        date.toDateTime()?.let {
            val relativeDate = RelativeDate.getRelativeDate(application, it);
            val ss = SpannableString("Due: " + relativeDate);
            if (date == application.today && useColor) {
                setColor(ss, dueTodayColor);
            } else if ((application.today.compareTo(date) > 0) && useColor) {
                setColor(ss, overDueColor);
            }
            return ss;
        }
    }
    return null;
}


private fun calculateRelativeAge(ctx: Context, dateString: String): String {
    val date = dateString.toDateTime()
    date?.let {
        return RelativeDate.getRelativeDate(ctx, date);
    }
    return dateString;
}

fun getRelativeAge(task: Task, ctx: Context): String? {
    val date = task.createDate
    date?.let {
        return (calculateRelativeAge(ctx, date));
    }
    return null;
}

fun initTaskWithFilter(task: Task, mFilter : ActiveFilter) {
    if (!mFilter.contextsNot && mFilter.contexts.size == 1) {
        task.addList(mFilter.contexts[0]);
    }

    if (!mFilter.projectsNot && mFilter.projects.size == 1) {
        task.addTag(mFilter.projects[0]);
    }
}

fun String.toDateTime(): DateTime? {
    var date: DateTime?;
    if ( DateTime.isParseable(this)) {
        date = DateTime(this)
    } else {
        date = null
    }
    return date
}

fun ArrayList<HashSet<String>>.union() : Set<String> {
    val result = this.fold  (HashSet<String>()) {
        left, right ->
        left.addAll(right)
        left
    }
    return result
}

fun ArrayList<HashSet<String>>.intersection() : Set<String> {
    val intersection = this.firstOrNull()?.toHashSet() ?: return emptySet()
    for (i in 1..this.lastIndex) {
        intersection.retainAll(this[i])
        if (intersection.isEmpty()) {
            break
        }
    }
    return intersection
}