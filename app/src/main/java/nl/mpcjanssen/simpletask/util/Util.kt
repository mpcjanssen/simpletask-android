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
import android.content.DialogInterface
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
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
import nl.mpcjanssen.simpletask.task.Task
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.io.*
import java.nio.channels.FileChannel
import java.util.*
import java.util.regex.Pattern

val TAG = "Util"
val log = Logger
val todayAsString: String
    get() = DateTime.today(TimeZone.getDefault()).format(Constants.DATE_FORMAT)

val mdParser: Parser = Parser.builder().build()
val htmlRenderer : HtmlRenderer = HtmlRenderer.builder().build()

fun runOnMainThread(r: Runnable) {
    val handler = Handler(Looper.getMainLooper())
    handler.post(r)
}

fun getString (resId : Int) : String {
    return TodoApplication.app.getString(resId)
}

fun showConfirmationDialog(cxt: Context,
                           msgid: Int,
                           okListener: DialogInterface.OnClickListener,
                           titleid: Int) {
    val builder = AlertDialog.Builder(cxt)
    builder.setTitle(titleid)
    showConfirmationDialog(msgid, okListener, builder)
}

fun showConfirmationDialog(cxt: Context,
                           msgid: Int,
                           okListener: DialogInterface.OnClickListener,
                           title: CharSequence) {
    val builder = AlertDialog.Builder(cxt)
    builder.setTitle(title)
    showConfirmationDialog(msgid, okListener, builder)
}

private fun showConfirmationDialog(msgid: Int,
                           okListener: DialogInterface.OnClickListener,
                           builder: AlertDialog.Builder) {
    val show = Config.showConfirmationDialogs
    builder.setMessage(msgid)
    builder.setPositiveButton(android.R.string.ok, okListener)
    builder.setNegativeButton(android.R.string.cancel, null)
    builder.setCancelable(true)
    val dialog = builder.create()
    if (show) {
        dialog.show()
    } else {
        okListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
    }
}

fun showToastShort(cxt: Context, resid: Int) {
    runOnMainThread(Runnable { Toast.makeText(cxt, resid, Toast.LENGTH_SHORT).show() })
}

@Suppress("unused")
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
    val log = Logger
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

fun addHeaderLines(visibleTasks: Sequence<Task>, sorts: List<String>, no_header: String, createIsThreshold : Boolean, moduleName : String?): List<VisibleLine> {
    var firstGroupSortIndex = 0
    if (sorts.size > 1 && sorts[0].contains("completed") || sorts[0].contains("future")) {
        firstGroupSortIndex++
        if (sorts.size > 2 && sorts[1].contains("completed") || sorts[1].contains("future")) {
            firstGroupSortIndex++
        }
    }
    val firstSort = sorts[firstGroupSortIndex]

    var header = ""
    val result = ArrayList<VisibleLine>()
    var count = 0
    var headerLine: HeaderLine? = null
    val luaGrouping = moduleName != null && LuaInterpreter.hasOnGroupCallback(moduleName)
    for (item in visibleTasks) {
        val t = item
        val newHeader = if ( moduleName!=null && luaGrouping ) {
            LuaInterpreter.onGroupCallback(moduleName, t)
        } else {
            null
        } ?: t.getHeader(firstSort, no_header, createIsThreshold)
        if (header != newHeader) {
            if (headerLine != null) {
                headerLine.title += " ($count)"
            }
            headerLine = HeaderLine(newHeader)
            count = 0
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

fun addHeaderLines(visibleTasks: Sequence<Task>, filter: ActiveFilter, no_header: String): List<VisibleLine> {
    val sorts = filter.getSort(Config.defaultSorts)
    return addHeaderLines(visibleTasks, sorts, no_header, filter.createIsThreshold, filter.options.luaModule)
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

fun addBusinessDays(originalDate: DateTime, days: Int): DateTime {
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
    } // Dont add anything
    return newDate
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

fun createAlertDialog(act: Activity, titleId: Int, alert: String): AlertDialog {
    val builder = AlertDialog.Builder(act)
    builder.setTitle(titleId)
    builder.setPositiveButton(R.string.ok, null)
    builder.setMessage(alert)
    return builder.create()
}

fun createDeferDialog(act: Activity, titleId: Int, listener: InputDialogListener): AlertDialog {
    val keys = act.resources.getStringArray(R.array.deferOptions)
    val today = "0d"
    val tomorrow = "1d"
    val oneWeek = "1w"
    val twoWeeks = "2w"
    val oneMonth = "1m"
    val values = arrayOf("", today, tomorrow, oneWeek, twoWeeks, oneMonth, "pick")

    val builder = AlertDialog.Builder(act)
    builder.setTitle(titleId)
    builder.setItems(keys) { dialog, whichButton ->
        val which = whichButton
        val selected = values[which]
        listener.onClick(selected)
    }
    return builder.create()
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

fun alfaSortList(items: List<String>, caseSensitive: Boolean, prefix: String?): ArrayList<String> {
    val result = ArrayList<String>()
    result .addAll(items.sortedWith ( compareBy<String> {
        if (caseSensitive) it.toLowerCase(Locale.getDefault()) else it
    }))
    if (prefix != null) {
        result.add(0, prefix)
    }
    return result
}

fun alfaSortList(items: Set<String>, caseSensitive: Boolean, prefix: String? = null): ArrayList<String> {
    val temp = ArrayList<String>()
    temp.addAll(items)
    return alfaSortList(temp, caseSensitive, prefix)
}

fun appVersion(ctx: Context): String {
    val packageInfo = ctx.packageManager.getPackageInfo(
            ctx.packageName, 0)
    return "Simpletask " + BuildConfig.FLAVOR + " v" + packageInfo.versionName + " (" + BuildConfig.VERSION_CODE + ")"
}

fun shortAppVersion(): String {
    return "${BuildConfig.FLAVOR.first()}${BuildConfig.VERSION_CODE}"
}

fun shareText(act: Activity, subject: String, text: String) {

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
        if (visibleDialog != null) {
            visibleDialog.show()
            return visibleDialog
        }
        val newDialog = Dialog(act)
        newDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        newDialog.setContentView(R.layout.loading)
        val pr = newDialog.findViewById(R.id.progress) as ProgressBar?
        pr?.indeterminateDrawable?.setColorFilter(-16737844, android.graphics.PorterDuff.Mode.MULTIPLY)
        newDialog.window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        newDialog.setCancelable(false)
        newDialog.show()
        return newDialog
    } else if (visibleDialog != null && visibleDialog.isShowing) {
        visibleDialog.dismiss()
    }
    return null
}

fun showChangelogOverlay(act: Activity): Dialog? {
    val builder = AlertDialog.Builder(act)
    builder.setMessage(readAsset(act.assets, "changelog.en.md"))
    builder.setCancelable(true)
    builder.setPositiveButton("OK", null)
    val dialog = builder.create()
    dialog.show()
    return dialog
}

fun markdownAssetAsHtml(ctxt: Context, name: String): String {
    var markdown: String
    try {
        markdown = readAsset(ctxt.assets, name)
    } catch (e: IOException) {
        val fallbackAsset = name.replace("\\.[a-z]{2}\\.md$".toRegex(), ".en.md")
        log.warn(TAG, "Failed to load markdown asset: $name falling back to $fallbackAsset")
        try {
            markdown = readAsset(ctxt.assets, fallbackAsset)
        } catch (e: IOException) {
            markdown = "$name and fallback $fallbackAsset not found."
        }
    }
    // Change issue numbers to links
    markdown = markdown.replace("(\\s)(#)([0-9]+)".toRegex(), "$1[$2$3](https://github.com/mpcjanssen/simpletask-android/issues/$3)")
    val document = mdParser.parse(markdown)
    val html =
            """
            <html>
            <head>
            <link rel='stylesheet' type='text/css' href='css/base.css'>
            <link rel='stylesheet' type='text/css' href='css/${getCssTheme()}'>
            </head><body>
            ${htmlRenderer.render(document)}
            </body></html>
            """.trimIndent()
    return html
}

fun getCssTheme(): String {
    if (Config.isBlackTheme) return "black.css"
    if (Config.isDarkTheme) return "dark.css"
    return "light.css"
}

@Throws(IOException::class)
fun readAsset(assets: AssetManager, name: String): String {
    val buf = StringBuilder()
    val input = assets.open(name)
    val `in` = BufferedReader(InputStreamReader(input))
    `in`.forEachLine {
        buf.append(it).append("\n")
    }

    `in`.close()
    return buf.toString()
}

fun getRelativeThresholdDate(task: Task, app: TodoApplication): String? {
    val date = task.thresholdDate ?: return null
    return getRelativeDate(app, "T: ", date).toString()
}

fun getRelativeDueDate(task: Task, app: TodoApplication): SpannableString? {
    val date = task.dueDate ?: return null
    return getRelativeDate(app, "Due: ", date)
}

/**
 * This method returns a String representing the relative date by comparing
 * the Calendar being passed in to the date / time that it is right now. It
 * will compute both past and future relative dates. E.g., "one day ago" and
 * "one day from now".
 *

 *
 * **NOTE:** If the calendar date relative to "now" is older
 * than one day, we display the actual date in its default format as
 * specified by this class. If you don't want to
 * show the actual date, but you want to show the relative date for days,
 * months, and years, you can add the other cases in by copying the logic
 * for hours, minutes, seconds.

 * @param dateString date to calculate difference to
 * *
 * @return String representing the relative date
 */

private fun getRelativeDate(app: TodoApplication, prefix: String, dateString: String): SpannableString? {
    val date = dateString.toDateTime() ?: return null
    val now = DateTime.today(TimeZone.getDefault())
    val days = date.numDaysFrom(now)
    val months = days / 31
    val weeks = days / 7
    val years = days / 365
    val s = when {
        years == 1 -> app.getString(R.string.dates_one_year_ago)
        years > 1 -> app.getString(R.string.dates_years_ago, years)
        months == 1 -> app.getString(R.string.dates_one_month_ago)
        months > 1 -> app.getString(R.string.dates_months_ago, months)
        weeks == 1 -> app.getString(R.string.dates_one_week_ago)
        weeks > 1 -> app.getString(R.string.dates_weeks_ago, weeks)
        days == 1 -> app.getString(R.string.dates_one_day_ago)
        days > 1 -> app.getString(R.string.dates_days_ago, days)
        days == 0 -> app.getString(R.string.dates_today)
        days == -1 -> app.getString(R.string.dates_tomorrow)
        else -> date.toString()
    }

    val ss = SpannableString(prefix + s)

    if (Config.hasColorDueDates && prefix == "Due: ") {
        val dueTodayColor = ContextCompat.getColor(app, R.color.simple_green_light)
        val overDueColor = ContextCompat.getColor(app, R.color.simple_red_light)
        val dueTomorrowColor = ContextCompat.getColor(app, R.color.simple_blue_light)
        when {
            days == 0 -> setColor(ss, dueTodayColor)
            date.lteq(now) -> setColor(ss, overDueColor)
            days == -1 -> setColor(ss, dueTomorrowColor)
        }
    }

    return ss
}

fun getRelativeAge(task: Task, app: TodoApplication): String? {
    val date = task.createDate ?: return null
    return getRelativeDate(app, "", date).toString()
}

fun initTaskWithFilter(task: Task, mFilter: ActiveFilter) {
    if (!mFilter.contextsNot && mFilter.contexts.size == 1) {
        task.addList(mFilter.contexts[0])
    }

    if (!mFilter.projectsNot && mFilter.projects.size == 1) {
        task.addTag(mFilter.projects[0])
    }
}

fun String.toDateTime(): DateTime? {
    val date: DateTime?
    if (DateTime.isParseable(this)) {
        date = DateTime(this)
    } else {
        date = null
    }
    return date
}

fun ArrayList<HashSet<String>>.union(): Set<String> {
    val result = this.fold(HashSet<String>()) {
        left, right ->
        left.addAll(right)
        left
    }
    return result
}

fun ArrayList<HashSet<String>>.intersection(): Set<String> {
    val intersection = this.firstOrNull()?.toHashSet() ?: return emptySet()
    for (i in 1..this.lastIndex) {
        intersection.retainAll(this[i])
        if (intersection.isEmpty()) {
            break
        }
    }
    return intersection
}

fun broadcastFileSync(broadcastManager: LocalBroadcastManager) {
    log.info(TAG, "Sending file changed broadcast")
    broadcastManager.sendBroadcast(Intent(Constants.BROADCAST_FILE_SYNC))
}

fun broadcastFileSyncStart(broadcastManager: LocalBroadcastManager) {
    log.info(TAG, "Sending file sync start broadcast")
    broadcastManager.sendBroadcast(Intent(Constants.BROADCAST_SYNC_START))
}

fun broadcastFileSyncDone(broadcastManager: LocalBroadcastManager) {
    log.info(TAG, "Sending file sync done changed broadcast")
    broadcastManager.sendBroadcast(Intent(Constants.BROADCAST_SYNC_DONE))
}

fun broadcastRefreshUI(broadcastManager: LocalBroadcastManager) {
    broadcastManager.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_UI))
}

fun broadcastTasklistChanged(broadcastManager: LocalBroadcastManager) {
    broadcastManager.sendBroadcast(Intent(Constants.BROADCAST_TASKLIST_CHANGED))
}

fun broadcastRefreshSelection(broadcastManager: LocalBroadcastManager) {
    broadcastManager.sendBroadcast(Intent(Constants.BROADCAST_HIGHLIGHT_SELECTION))
}

fun broadcastRefreshWidgets(broadcastManager: LocalBroadcastManager) {
    log.info(TAG, "Sending widget refresh broadcast")
    broadcastManager.sendBroadcast(Intent(Constants.BROADCAST_UPDATE_WIDGETS))
}
