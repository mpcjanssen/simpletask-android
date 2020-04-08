/**

 * Copyright (c) 2015 Vojtech Kral

 * LICENSE:

 * Simpletask is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.

 * Simpletask is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.

 * You should have received a copy of the GNU General Public License along with Sinpletask.  If not, see
 * //www.gnu.org/licenses/>.

 * @author Vojtech Kral
 * *
 * @license http://www.gnu.org/licenses/gpl.html
 * *
 * @copyright 2015 Vojtech Kral
 */

package nl.mpcjanssen.simpletask

import java.util.*
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentValues
import android.content.ContentProviderOperation
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.provider.CalendarContract.*
import androidx.core.content.ContextCompat
import android.util.Log
import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.task.*
import nl.mpcjanssen.simpletask.util.toDateTime

private enum class EvtStatus {
    KEEP,
    DELETE,
    INSERT,
}

data class EvtKey(val dtStart: Long, val title: String)

private class Evt(
        var id: Long,
        var dtStart: Long,
        var title: String,
        var description: String,
        var remID: Long = -1,
        var remMinutes: Long = -1,
        var status: EvtStatus = EvtStatus.DELETE) {

    constructor(cursor: Cursor) // Create from a db record
            : this(cursor.getLong(0), cursor.getLong(2), cursor.getString(3), cursor.getString(4))

    constructor(date: DateTime, title: String, desc: String) // Create from a Task
            : this(-1, date.getMilliseconds(CalendarSync.UTC), title, desc, -1, -1, EvtStatus.INSERT) {

        val localZone = Calendar.getInstance().timeZone
        val remMargin = TodoApplication.config.reminderDays * 1440
        val remTime = TodoApplication.config.reminderTime
        val remDT = DateTime.forTimeOnly(remTime / 60, remTime % 60, 0, 0)

        // Reminder data:
        // Only create reminder if it's in the future, otherwise it would go off immediately
        // NOTE: DateTime.minus()/plus() only accept values >=0 and <10000 (goddamnit date4j!), hence the division.
        var remDate = date.minus(0, 0, 0, remMargin / 60, remMargin % 60, 0, 0, DateTime.DayOverflow.Spillover)
        remDate = remDate.plus(0, 0, 0, remDT.hour, remDT.minute, 0, 0, DateTime.DayOverflow.Spillover)
        if (remDate.isInTheFuture(localZone)) {
            remID = 0L // 0 = reminder entry to be created
            remMinutes = remDate.numSecondsFrom(date) / 60
        }
    }

    fun key() = EvtKey(dtStart, title)

    override fun hashCode(): Int {
        var result = dtStart.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + remMinutes.hashCode()
        return result
    }

    override fun equals(other: Any?) =
            if (other !is Evt) {
                false
            }  else {
            dtStart == other.dtStart &&
                    title == other.title &&
                    description == other.description &&
                    remMinutes == other.remMinutes }

    @TargetApi(16)
    fun addOp(list: ArrayList<ContentProviderOperation>, calID: Long) {
        if (status == EvtStatus.DELETE) {
            val args = arrayOf(id.toString())
            list.add(ContentProviderOperation.newDelete(Events.CONTENT_URI)
                    .withSelection(Events._ID + "=?", args)
                    .build())
            if (remID >= 0) {
                val remArgs = arrayOf(remID.toString())
                list.add(ContentProviderOperation.newDelete(Reminders.CONTENT_URI)
                        .withSelection(Reminders._ID + "=?", remArgs)
                        .build())
            }
        } else if (status == EvtStatus.INSERT) {
            list.add(ContentProviderOperation.newInsert(Events.CONTENT_URI)
                    .withValue(Events.CALENDAR_ID, calID)
                    .withValue(Events.TITLE, title)
                    .withValue(Events.DTSTART, dtStart)
                    .withValue(Events.DTEND, dtStart + 24 * 60 * 60 * 1000) // Needs to be set to DTSTART +24h, otherwise reminders don't work
                    .withValue(Events.ALL_DAY, 1)
                    .withValue(Events.DESCRIPTION, description)
                    .withValue(Events.EVENT_TIMEZONE, CalendarSync.UTC.id)
                    .withValue(Events.STATUS, Events.STATUS_CONFIRMED)
                    .withValue(Events.HAS_ATTENDEE_DATA, true) // If this is not set, Calendar app is confused about Event.STATUS
                    .withValue(Events.CUSTOM_APP_PACKAGE, TodoApplication.app.packageName)
                    .withValue(Events.CUSTOM_APP_URI, Uri.withAppendedPath(Simpletask.URI_SEARCH, title).toString())
                    .build())
            if (remID >= 0) {
                val evtIdx = list.size - 1
                list.add(ContentProviderOperation.newInsert(Reminders.CONTENT_URI)
                        .withValueBackReference(Reminders.EVENT_ID, evtIdx)
                        .withValue(Reminders.MINUTES, remMinutes)
                        .withValue(Reminders.METHOD, Reminders.METHOD_ALERT)
                        .build())
            }
        }
    }
}

private class SyncStats(val inserts: Long, val keeps: Long, val deletes: Long)

/**
 * A hashmap of Evts
 *
 * In order to make calendar notifications sync more efficient, we want to only add/remove those
 * that actually need to be added/removed. However, Simpletask doesn't track task identity
 * and consequently we don't know what changed, not to mention todo.txt might've also
 * been changed outside of Simpletask.
 *
 * And so we need to diff between task list and what's actually in the calendar.
 * To do that, this hashmap is used, which stores events (Evt) by their date and text (EvtKey).
 * (Note that there might be multiple equal events, hence the LinkedList as the map value type.)
 * When constructed, the hashmap loads events from the DB, all of which are initially marked for deletion.
 * Then, task list is merged in: Each event (that comes from a task) is located in the hashmap
 * - if an equal one is found, it is marked to be kept unchanged.
 * If not, new event is inserted into the map and marked for insertion.
 *
 * Finally, the hashmap contents are applied - contained events are iterated and inserted/deleted as appropriate.
 * This is done using ContentResolver.applyBatch for better efficiency.
 */
@SuppressLint("Recycle", "NewAPI")
private class EvtMap private constructor() : HashMap<EvtKey, LinkedList<Evt>>() {
    @SuppressLint("MissingPermission")
    constructor(cr: ContentResolver, calID: Long) : this() {
        val evtPrj = arrayOf(Events._ID, Events.CALENDAR_ID, Events.DTSTART, Events.TITLE, Events.DESCRIPTION)
        val evtSel = "${Events.CALENDAR_ID} = ?"
        val evtArgs = arrayOf(calID.toString())

        val remPrj = arrayOf(Reminders._ID, Reminders.EVENT_ID, Reminders.MINUTES)
        val remSel = "${Reminders.EVENT_ID} = ?"

        val evts = cr.query(Events.CONTENT_URI, evtPrj, evtSel, evtArgs, null)
                ?: throw IllegalArgumentException("null cursor")
        while (evts.moveToNext()) {
            val evt = Evt(evts)

            // Try to find a matching reminder
            val remArgs = arrayOf(evt.id.toString())
            val rem = cr.query(Reminders.CONTENT_URI, remPrj, remSel, remArgs, null)
                    ?: throw IllegalArgumentException("null cursor")
            if (rem.count > 0) {
                rem.moveToFirst()
                evt.remID = rem.getLong(0)
                evt.remMinutes = rem.getLong(2)
            }
            rem.close()

            // Insert into the hashmap
            val evtkey = evt.key()
            var list = this.get(evtkey)
            if (list != null) list.add(evt)
            else {
                list = LinkedList<Evt>()
                list.add(evt)
                this.put(evtkey, list)
            }
        }
        evts.close()

    }

    fun mergeEvt(evt: Evt) {
        val key = evt.key()
        val list = this.get(key)
        evt.status = EvtStatus.INSERT
        if (list == null) {
            val nlist = LinkedList<Evt>()
            nlist.add(evt)
            this.put(key, nlist)
        } else {
            for (oevt in list) {
                if (oevt.status == EvtStatus.DELETE && oevt == evt) {
                    oevt.status = EvtStatus.KEEP
                    return
                }
            }
            list.add(evt)
        }
    }

    fun mergeTask(task: Task) {

            if (task.isCompleted()) return

            var text: String? = null

            // Check due date:
            var dt = task.dueDate?.toDateTime()
            if (TodoApplication.config.isSyncDues && dt != null) {
                text = task.showParts(CalendarSync.TASK_TOKENS)
                val evt = Evt(dt, text, TodoApplication.app.getString(R.string.calendar_sync_desc_due))
                mergeEvt(evt)
            }

            // Check threshold date:
            dt = task.thresholdDate?.toDateTime()
            if (TodoApplication.config.isSyncThresholds && dt != null) {
                if (text == null) text = task.showParts(CalendarSync.TASK_TOKENS)
                val evt = Evt(dt, text, TodoApplication.app.getString(R.string.calendar_sync_desc_thre))
                mergeEvt(evt)
            }
    }

    @SuppressLint("NewApi")
    fun apply(cr: ContentResolver, calID: Long): SyncStats {
        val ops = ArrayList<ContentProviderOperation>()
        var ins = 0L
        var kps = 0L
        var dels = 0L

        for (list in values) {
            for (evt in list) {
                when {
                    evt.status == EvtStatus.INSERT -> ins++
                    evt.status == EvtStatus.KEEP -> kps++
                    evt.status == EvtStatus.DELETE -> dels++
                }

                evt.addOp(ops, calID)
            }
        }

        cr.applyBatch(AUTHORITY, ops)
        return SyncStats(ins, kps, dels)
    }
}

object CalendarSync {

    private val ACCOUNT_NAME = "Simpletask Calendar"
    private val ACCOUNT_TYPE = ACCOUNT_TYPE_LOCAL
    private val CAL_URI = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .build()
    private val CAL_NAME = "simpletask_reminders_v34SsjC7mwK9WSVI"
    private val CAL_COLOR = Color.BLUE // Chosen arbitrarily...

    private val SYNC_DELAY_MS = 20 * 1000
    private val TAG = "CalendarSync"

    val UTC: TimeZone = TimeZone.getTimeZone("UTC")
    val TASK_TOKENS: (TToken) -> Boolean = {
        when (it) {
            is CompletedToken, is CreateDateToken, is CompletedDateToken, is PriorityToken, is ThresholdDateToken,
            is DueDateToken, is HiddenToken, is RecurrenceToken -> false
            else -> true
        }
    }

    private class SyncRunnable : Runnable {
        override fun run() {
            try {
                sync()
            } catch (e: Exception) {
                Log.e(TAG, "STPE exception", e)
            }

        }
    }

    private val m_sync_runnable: SyncRunnable
    private val m_cr: ContentResolver
    private val m_stpe: ScheduledThreadPoolExecutor

    @SuppressLint("Recycle")
    private fun findCalendar(): Long {
        val projection = arrayOf(Calendars._ID, Calendars.NAME)
        val selection = Calendars.NAME + " = ?"
        val args = arrayOf(CAL_NAME)
        /* Check for calendar permission */
        val permissionCheck = ContextCompat.checkSelfPermission(TodoApplication.app,
                Manifest.permission.WRITE_CALENDAR)

        if (permissionCheck == PackageManager.PERMISSION_DENIED) {
            if (TodoApplication.config.isSyncDues || TodoApplication.config.isSyncThresholds) {
                throw IllegalStateException("no calendar access")
            } else {
                return -1
            }
        }

        val cursor = m_cr.query(CAL_URI, projection, selection, args, null)
                ?: throw IllegalArgumentException("null cursor")
        if (cursor.count == 0) {
            cursor.close()
            return -1
        }
        cursor.moveToFirst()
        val ret = cursor.getLong(0)
        cursor.close()
        return ret
    }

    private fun addCalendar() {
        val cv = ContentValues()
        cv.apply {
            put(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            put(Calendars.NAME, CAL_NAME)
            put(Calendars.CALENDAR_DISPLAY_NAME, TodoApplication.app.getString(R.string.calendar_disp_name))
            put(Calendars.CALENDAR_COLOR, CAL_COLOR)
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ)
            put(Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
        }
        m_cr.insert(CAL_URI, cv)
    }

    @SuppressLint("NewApi")
    private fun removeCalendar() {
        Log.d(TAG, "Removing Simpletask calendar")
        val selection = Calendars.NAME + " = ?"
        val args = arrayOf(CAL_NAME)
        try {
            val ret = m_cr.delete(CAL_URI, selection, args)
            when (ret) {
                0 -> Log.d(TAG, "No calendar to remove")
                1 -> Log.d(TAG, "Calendar removed")
                else -> Log.e(TAG, "Unexpected return value while removing calendar: " + ret)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while removing calendar", e)
        }
    }

    private fun sync() {
        Log.d(TAG, "Checking whether calendar sync is needed")
        try {
            var calID = findCalendar()

            if (!TodoApplication.config.isSyncThresholds && !TodoApplication.config.isSyncDues) {
                if (calID >= 0) {
                    Log.d(TAG, "Calendar sync not enabled")
                    removeCalendar()
                }
                return
            }

            if (calID < 0) {
                addCalendar()
                calID = findCalendar() // Re-find the calendar, this is needed to verify it has been added
                if (calID < 0) {
                    // This happens when CM privacy guard disallows to write calendar (1)
                    // OR it allows to write calendar but disallows reading it (2).
                    // Either way, we cannot continue, but before bailing,
                    // try to remove Calendar in case we're here because of (2).
                    Log.d(TAG, "No access to Simpletask calendar")
                    removeCalendar()
                    throw IllegalStateException("Calendar nor added")
                }
            }

            Log.d(TAG, "Syncing due/threshold calendar reminders...")
            val evtmap = EvtMap(m_cr, calID)
            TodoApplication.todoList.each {
                evtmap.mergeTask(it)
            }
            val stats = evtmap.apply(m_cr, calID)
            Log.d(TAG, "Sync finished: ${stats.inserts} inserted, ${stats.keeps} unchanged, ${stats.deletes} deleted")
        } catch (e: SecurityException) {

            Log.e(TAG, "No calendar access permissions granted", e )
        } catch (e: Exception) {
            Log.e(TAG, "Calendar error", e)
        }
    }

    fun updatedSyncTypes() {
        syncLater()
    }

    init {
        m_sync_runnable = SyncRunnable()
        m_cr = TodoApplication.app.contentResolver
        m_stpe = ScheduledThreadPoolExecutor(1)
    }

    fun syncLater() {
        m_stpe.queue.clear()
        m_stpe.schedule(m_sync_runnable, SYNC_DELAY_MS.toLong(), TimeUnit.MILLISECONDS)
    }
}

