// TODO: is file header ok?
/**
 *
 * Copyright (c) 2009-2015 Todo.txt contributors (http://todotxt.com)
 * Copyright (c) 2013- Mark Janssen
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
 * @author Todo.txt contributors <todotxt@yahoogroups.com>
 * @license http://www.gnu.org/licenses/gpl.html
 * @copyright 2009-2015 Todo.txt contributors (http://todotxt.com)
 * @copyright 2013- Mark Janssen
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
import android.text.format.Time;
import android.util.Log;

import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Task;


public class CalendarSync {
    private static final String TAG = CalendarSync.class.getSimpleName();
    private static final String PACKAGE = TodoApplication.getAppContext().getPackageName();
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private static final String ACCOUNT_NAME = "Simpletask Calendar";
    private static final String ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL;
    private static final String CAL_NAME = "simpletask_reminders";
    private static final int CAL_COLOR = Color.BLUE;     // Chosen arbitrarily...

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
    private boolean m_enabled = false;
    private Uri m_cal_uri;

    private int m_margin_minutes = 1440;
    private ScheduledThreadPoolExecutor m_stpe;

    private static Uri buildCalUri() {
        return Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE).build();
    }

    private long getCalID() {
        final String[] projection = {Calendars._ID, Calendars.NAME};
        final String selection = Calendars.NAME+" = ?";
        Cursor cursor = m_cr.query(m_cal_uri, projection, selection, new String[]{CAL_NAME}, null);
        if (cursor == null) return -1;
        if (cursor.getCount() == 0) return -1;
        cursor.moveToFirst();
        long ret = cursor.getLong(0);
        cursor.close();
        return ret;
    }

    private boolean addCalendar() {
        final ContentValues cv = new ContentValues();
        cv.put(Calendars.ACCOUNT_NAME, ACCOUNT_NAME);
        cv.put(Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE);
        cv.put(Calendars.NAME, CAL_NAME);
        cv.put(Calendars.CALENDAR_DISPLAY_NAME, m_app.getString(R.string.calendar_disp_name));
        cv.put(Calendars.CALENDAR_COLOR, CAL_COLOR);
        cv.put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER);
        cv.put(Calendars.OWNER_ACCOUNT, ACCOUNT_NAME);
        cv.put(Calendars.VISIBLE, 1);
        cv.put(Calendars.SYNC_EVENTS, 1);

        Uri calUri = m_cr.insert(m_cal_uri, cv);
        if (calUri == null) return false;
        else if (getCalID() == -1) return false;    // This might happen eg. because of CM's privacy guard
        return true;
    }

    private void removeCalendar() {
        String selection = Calendars.NAME+" = ?";
        int ret = m_cr.delete(m_cal_uri, selection, new String[]{CAL_NAME});
        Log.v(TAG, "Calendar removed: "+ret);
    }

    private void insertEvt(long calID, DateTime date, String title) {
        ContentValues values = new ContentValues();
        long millis = date.getMilliseconds(UTC);

        // Event:
        values.put(Events.CALENDAR_ID, calID);
        values.put(Events.TITLE, title);
        values.put(Events.DTSTART, millis);
        values.put(Events.DTEND, millis);
        values.put(Events.ALL_DAY, true);
        values.put(Events.DESCRIPTION, m_app.getString(R.string.calendar_sync_evt_desc));
        values.put(Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC);  // Doc: If allDay is set to 1, eventTimezone must be TIMEZONE_UTC
        values.put(Events.STATUS, Events.STATUS_CONFIRMED);
        values.put(Events.HAS_ATTENDEE_DATA, true);      // If this is not set, Calendar app is confused about Event.STATUS
        values.put(Events.CUSTOM_APP_PACKAGE, PACKAGE);
        // TODO: CUSTOM_APP_URI ?
        Uri uri = m_cr.insert(Events.CONTENT_URI, values);

        // Reminder:
        long evtID = Long.parseLong(uri.getLastPathSegment());
        values.clear();
        values.put(Reminders.EVENT_ID, evtID);
        values.put(Reminders.MINUTES, m_margin_minutes);
        values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
        m_cr.insert(Reminders.CONTENT_URI, values);
    }

    private void insertEvts(long calID, final List<Task> tasks) {
        for (Task task: tasks) {
            if (task.isCompleted()) continue;

            // Check due date:
            DateTime dt = task.getDueDate();
            if (dt != null) {
                String title = m_app.getString(R.string.calendar_sync_prefix_due)+' '+task.getText();
                insertEvt(calID, dt, title);
            }

            // Check threshold date:
            dt = task.getThresholdDate();
            if (dt != null) {
                String title = m_app.getString(R.string.calendar_sync_prefix_thre)+' '+task.getText();
                insertEvt(calID, dt, title);
            }
        }
    }

    private void purgeEvts(long calID) {
        final String selection = Events.CALENDAR_ID+" = ?";
        m_cr.delete(Events.CONTENT_URI, selection, new String[]{String.valueOf(calID)});
    }

    private void sync() {
        if (!m_enabled) return;

        final List<Task> tasks = m_app.getTaskCache().getTasks();
        setRemindersMarginDays(m_app.getRemindersMarginDays());

        long calID = getCalID();
        if (calID == -1) {
            Log.e(TAG, "sync(): No calendar!");
            return;
        }

        Log.v(TAG, "Syncing due & threshold calendar reminders...");
        purgeEvts(calID);
        insertEvts(calID, tasks);
    }


    public CalendarSync(TodoApplication app, boolean enabled) {
        m_app = app;
        m_sync_runnable = new SyncRunnable();
        m_cr = app.getContentResolver();
        m_cal_uri = buildCalUri();
        m_stpe = new ScheduledThreadPoolExecutor(1);
        setEnabled(enabled);
    }

    public void syncLater() {
        if (!m_enabled) return;

        m_stpe.getQueue().clear();
        m_stpe.schedule(m_sync_runnable, SYNC_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public boolean isEnabled() {
        return m_enabled;
    }

    public void setEnabled(boolean enabled) {
        m_enabled = enabled;
        long calID = getCalID();
        if (m_enabled && calID == -1) {
            if (addCalendar()) {
                Log.v(TAG, "Added calendar");
            } else {
                m_enabled = false;
                Log.e(TAG, "Could not add calendar");
            }
        } else if (!m_enabled && calID != -1) {
            removeCalendar();
        }
    }

    public void setRemindersMarginDays(int days) {
        m_margin_minutes = days * 1440 - 720;    // -720 to make the reminder go off during the day
    }
}
