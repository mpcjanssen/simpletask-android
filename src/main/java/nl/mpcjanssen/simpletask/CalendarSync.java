/**
 *
 * Copyright (c) 2015 Vojtech Kral
 *
 * LICENSE:
 *
 * Simpletas is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 2 of the License, or (at your option) any
 * later version.
 *
 * Simpletask is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with Sinpletask.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * @author Vojtech Kral
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2015 Vojtech Kral
 */

package nl.mpcjanssen.simpletask;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.util.Util;


public class CalendarSync {
    private static final String TAG = CalendarSync.class.getSimpleName();
    private static final String PACKAGE = TodoApplication.getAppContext().getPackageName();
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final String ACCOUNT_NAME = "Simpletask Calendar";
    private static final String ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL;
    private static final Uri CAL_URI = Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE).build();
    private static final String CAL_NAME = "simpletask_reminders_v34SsjC7mwK9WSVI";
    private static final int CAL_COLOR = Color.BLUE;       // Chosen arbitrarily...
    private static final int EVT_DURATION = 5*60*60*1000;  // ie. 5 hours

    private static final int SYNC_DELAY_MS = 1000;

    private class SyncRunnable implements Runnable {
        @Override
        public void run() {
            CalendarSync.this.sync();
        }
    }

    private TodoApplication m_app;
    private SyncRunnable m_sync_runnable;
    private ContentResolver m_cr;
    private int m_sync_type;
    private int m_rem_margin = 1440;
    private DateTime m_rem_time = DateTime.forTimeOnly(12, 0, 0, 0);
    private ScheduledThreadPoolExecutor m_stpe;

    private long getCalID() {
        final String[] projection = {Calendars._ID, Calendars.NAME};
        final String selection = Calendars.NAME+" = ?";
        final String[] args = {CAL_NAME};
        Cursor cursor = m_cr.query(CAL_URI, projection, selection, args, null);
        if (cursor == null) return -1;
        if (cursor.getCount() == 0) return -1;
        cursor.moveToFirst();
        long ret = cursor.getLong(0);
        cursor.close();
        return ret;
    }

    private void addCalendar(boolean warnCalExists) {
        if (getCalID() != -1) {
            if (warnCalExists) {
                Log.w(TAG, "Calendar already exists, overwriting...");
                Util.showToastShort(TodoApplication.getAppContext(), R.string.calendar_exists_warning);
            }
            return;
        }

        final ContentValues cv = new ContentValues();
        cv.put(Calendars.ACCOUNT_NAME, ACCOUNT_NAME);
        cv.put(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE);
        cv.put(Calendars.NAME, CAL_NAME);
        cv.put(Calendars.CALENDAR_DISPLAY_NAME, m_app.getString(R.string.calendar_disp_name));
        cv.put(Calendars.CALENDAR_COLOR, CAL_COLOR);
        cv.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_READ);
        cv.put(Calendars.OWNER_ACCOUNT, ACCOUNT_NAME);
        cv.put(Calendars.VISIBLE, 1);
        cv.put(Calendars.SYNC_EVENTS, 1);

        Uri calUri = m_cr.insert(CAL_URI, cv);
        boolean added = true;
        if (calUri == null) added = false;
        else if (getCalID() == -1) added = false;    // This might happen eg. because of CM's privacy guard

        if (added) {
            Log.v(TAG, "Calendar added");
        }
        else {
            Log.e(TAG, "Could not add calendar");
            m_sync_type = 0;
        }
    }

    private void removeCalendar() {
        final String selection = Calendars.NAME+" = ?";
        final String[] args = {CAL_NAME};
        int ret = m_cr.delete(CAL_URI, selection, args);
        if (ret == 1) Log.v(TAG, "Calendar removed");
        else Log.w(TAG, "Unexpected return value while removing calendar: "+ret);
    }

    private void insertEvt(long calID, DateTime date, String titlePrefix, String title) {
        ContentValues values = new ContentValues();

        long dtstart = (new DateTime(date.getYear(), date.getMonth(), date.getDay(), m_rem_time.getHour(),
                m_rem_time.getMinute(), m_rem_time.getSecond(), m_rem_time.getNanoseconds()))
                .getMilliseconds(Calendar.getInstance().getTimeZone());

        // Event:
        values.put(Events.CALENDAR_ID, calID);
        values.put(Events.TITLE, titlePrefix+' '+title);
        values.put(Events.DTSTART, dtstart);
        values.put(Events.DTEND, dtstart + EVT_DURATION);
        values.put(Events.ALL_DAY, 0);
        values.put(Events.DESCRIPTION, m_app.getString(R.string.calendar_sync_evt_desc));
        values.put(Events.EVENT_TIMEZONE, UTC.getID());
        values.put(Events.STATUS, Events.STATUS_CONFIRMED);
        values.put(Events.HAS_ATTENDEE_DATA, true);      // If this is not set, Calendar app is confused about Event.STATUS
        values.put(Events.CUSTOM_APP_PACKAGE, PACKAGE);
        values.put(Events.CUSTOM_APP_URI, Uri.withAppendedPath(Simpletask.URI_SEARCH, title).toString());
        Uri uri = m_cr.insert(Events.CONTENT_URI, values);

        // Reminder:
        long evtID = Long.parseLong(uri.getLastPathSegment());
        values.clear();
        values.put(Reminders.EVENT_ID, evtID);
        values.put(Reminders.MINUTES, m_rem_margin);
        values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
        m_cr.insert(Reminders.CONTENT_URI, values);
    }

    private void insertEvts(long calID, final List<Task> tasks) {
        for (Task task: tasks) {
            if (task.isCompleted()) continue;

            DateTime dt;
            String text = null;

            // Check due date:
            if ((m_sync_type & SYNC_TYPE_DUES) != 0) {
                dt = task.getDueDate();
                if (dt != null) {
                    text = task.getText();
                    insertEvt(calID, dt, m_app.getString(R.string.calendar_sync_prefix_due), text);
                }
            }

            // Check threshold date:
            if ((m_sync_type & SYNC_TYPE_THRESHOLDS) != 0) {
                dt = task.getThresholdDate();
                if (dt != null) {
                    if (text == null) text = task.getText();
                    insertEvt(calID, dt, m_app.getString(R.string.calendar_sync_prefix_thre), text);
                }
            }
        }
    }

    private void purgeEvts(long calID) {
        final String selection = Events.CALENDAR_ID+" = ?";
        final String args[] = {""};
        args[0] = String.valueOf(calID);
        m_cr.delete(Events.CONTENT_URI, selection, args);
    }

    private void sync() {
        if (m_sync_type == 0) return;

        final List<Task> tasks = m_app.getTaskCache(null).getTasks();
        setReminderDays(m_app.getReminderDays());
        setReminderTime(m_app.getReminderTime());

        long calID = getCalID();
        if (calID == -1) {
            Log.e(TAG, "sync(): No calendar!");
            return;
        }

        Log.v(TAG, "Syncing due/threshold calendar reminders...");
        purgeEvts(calID);
        insertEvts(calID, tasks);
    }

    private void setSyncType(int syncType, boolean warnCalExists) {
        if (syncType == m_sync_type) return;
        int old_sync_type = m_sync_type;
        m_sync_type = syncType;

        if ((old_sync_type == 0) && (m_sync_type > 0)) {
            addCalendar(warnCalExists);
        }
        else if ((old_sync_type > 0) && (m_sync_type == 0)) {
            removeCalendar();
        }

        if (m_sync_type > 0) syncLater();
    }



    public static final int SYNC_TYPE_DUES = 1, SYNC_TYPE_THRESHOLDS = 2;

    public CalendarSync(TodoApplication app, boolean syncDues, boolean syncThresholds) {
        m_app = app;
        m_sync_runnable = new SyncRunnable();
        m_cr = app.getContentResolver();
        m_stpe = new ScheduledThreadPoolExecutor(1);
        int syncType = 0;
        if (syncDues) syncType |= SYNC_TYPE_DUES;
        if (syncThresholds) syncType |= SYNC_TYPE_THRESHOLDS;
        setSyncType(syncType, false);
    }

    public void syncLater() {
        if (m_sync_type == 0) return;

        m_stpe.getQueue().clear();
        m_stpe.schedule(m_sync_runnable, SYNC_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public void setSyncDues(boolean bool) {
        int syncType = bool ? m_sync_type | SYNC_TYPE_DUES : m_sync_type & ~SYNC_TYPE_DUES;
        setSyncType(syncType, true);
    }

    public void setSyncThresholds(boolean bool) {
        int syncType = bool ? m_sync_type | SYNC_TYPE_THRESHOLDS : m_sync_type & ~SYNC_TYPE_THRESHOLDS;
        setSyncType(syncType, true);
    }

    public void setReminderDays(int days) {
        m_rem_margin = days * 1440;
    }

    public void setReminderTime(int time) {
        m_rem_time = DateTime.forTimeOnly(time / 60, time % 60, 0, 0);
    }
}
