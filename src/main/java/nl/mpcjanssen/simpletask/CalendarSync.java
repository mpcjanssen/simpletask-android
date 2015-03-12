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
import android.net.Uri;
import android.preference.MultiSelectListPreference;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.task.Task;
import nl.mpcjanssen.simpletask.task.TaskCache;

public class CalendarSync {
    private static final String TAG = CalendarSync.class.getSimpleName();
    private static final String PACKAGE = TodoApplication.getAppContext().getPackageName();
    private static final boolean API16 = android.os.Build.VERSION.SDK_INT >= 16;
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final String PREFIX_DUE = "Task due: ";
    private static final String PREFIX_THRESHOLD = "Task threshold: ";
    private static final String EVT_DESCRIPTION = "Generated automatically by Simpletask";

    private ContentResolver m_cr;
    private List<Long> m_calendars;
    private Set<Long> m_evtset;
    private int m_margin_minutes = 1440;

    private boolean calendarValid(String id, String name) {
        Uri uri = Calendars.CONTENT_URI.buildUpon().appendPath(id).build();
        final String[] projection = {Calendars._ID, Calendars.NAME, Calendars.CALENDAR_DISPLAY_NAME};
        final String selection = Calendars.NAME+" = ?";
        Cursor cursor = m_cr.query(uri, projection, selection, new String[]{name}, null);
        boolean ret = cursor.getCount() == 1;
        cursor.close();
        return ret;
    }

    private void loadEvtSet(long calID) {
        m_evtset.clear();

        final String[] projection = {Events._ID, Events.CALENDAR_ID, Events.CUSTOM_APP_PACKAGE};
        final String selection = Events.CALENDAR_ID+" = ? AND "+Events.CUSTOM_APP_PACKAGE+" = ?";
        Cursor cursor = m_cr.query(Events.CONTENT_URI, projection, selection,
                new String[]{Long.toString(calID), PACKAGE}, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                m_evtset.add(cursor.getLong(0));
            }
            cursor.close();
        }
    }

    private long findEvt(long calID, DateTime date, String title) {
        final String[] projection = {Events._ID, Events.CALENDAR_ID, Events.CUSTOM_APP_PACKAGE,
                Events.DTSTART, Events.TITLE};
        final String selection = Events.CALENDAR_ID+" = ? AND "+Events.CUSTOM_APP_PACKAGE+" = ? AND "
                +Events.DTSTART+" = ? AND "+Events.TITLE+" = ?";
        Cursor cursor = m_cr.query(Events.CONTENT_URI, projection, selection,
                new String[]{Long.toString(calID), PACKAGE, String.valueOf(date.getMilliseconds(UTC)), title}, null);

        if (cursor == null) return -1;
        if (cursor.getCount() != 1) return -1;
        cursor.moveToFirst();
        long ret = cursor.getLong(0);
        cursor.close();
        return ret;
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
        values.put(Events.DESCRIPTION, EVT_DESCRIPTION);
        values.put(Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());
        values.put(Events.CUSTOM_APP_PACKAGE, PACKAGE);
        // TODO: CUSTOM_APP_URI ?
        Uri uri = m_cr.insert(Events.CONTENT_URI, values);

        // Reminder:
        long evtID = Long.parseLong(uri.getLastPathSegment());
        values.clear();
        values.put(Reminders.EVENT_ID, evtID);
        values.put(Reminders.MINUTES, m_margin_minutes);
        values.put(Reminders.METHOD, Reminders.METHOD_DEFAULT);
        m_cr.insert(Reminders.CONTENT_URI, values);
    }

    private void syncCal(long calID, final List<Task> tasks) {
        for (Task task: tasks) {
            if (task.isCompleted()) continue;

            // Check due date:
            DateTime dt = task.getDueDate();
            if (dt != null) {
                String title = PREFIX_DUE+task.getText();
                long id = findEvt(calID, dt, title);
                if (id >= 0) {
                    m_evtset.remove(id);  // Don't delete this event
                } else {
                    insertEvt(calID, dt, title);
                }
            }

            // Check threshold date:
            dt = task.getThresholdDate();
            if (dt != null) {
                String title = PREFIX_THRESHOLD+task.getText();
                long id = findEvt(calID, dt, title);
                if (id >= 0) {
                    m_evtset.remove(id);  // Don't delete this event
                } else {
                    insertEvt(calID, dt, title);
                }
            }
        }
    }

    private void purgeEvts() {
        final String selection = Events._ID+" = ?";
        for (Long id: m_evtset) {
            m_cr.delete(Events.CONTENT_URI, selection, new String[]{id.toString()});
        }
    }


    public CalendarSync(Set<String> calendars) {
        m_cr = TodoApplication.getAppContext().getContentResolver();
        m_calendars = new ArrayList<>();
        m_evtset = new HashSet<>();
        setSyncCalendars(calendars);
    }

    public void sync(TaskCache taskCache, int marginDays) {
        final List<Task> tasks = taskCache.getTasks();
        m_margin_minutes = marginDays * 1440;

        for (Long calID: m_calendars) {
            loadEvtSet(calID);
            syncCal(calID, tasks);
            purgeEvts();
        }
    }

    public void fillPrefCalendarList(MultiSelectListPreference pref) {
        List<String> entryValues = new ArrayList<>();
        List<String> entries = new ArrayList<>();

        final String[] projection = {Calendars._ID, Calendars.NAME, Calendars.CALENDAR_DISPLAY_NAME};
        Cursor cursor = m_cr.query(Calendars.CONTENT_URI, projection, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                // In an attempt to distinguish a calendar uniquely, both its ID and name is used:
                String value = cursor.getString(0) + ":" + cursor.getString(1);
                entryValues.add(value);
                entries.add(cursor.getString(2));
            }
        }

        pref.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
        pref.setEntries(entries.toArray(new CharSequence[entries.size()]));
    }

    public void setSyncCalendars(Set<String> calendars) {
        m_calendars.clear();

        // Custom events are supported only since JellyBean:
        if (!API16) return;

        for (String calendar: calendars) {
            String[] split = calendar.split(":", 2);
            if (calendarValid(split[0], split[1])) {
                try {
                    m_calendars.add(Long.parseLong(split[0]));
                } catch (NumberFormatException e) {
                    // Calendar ID not added
                }
            } else {
                Log.w(TAG, "Calendar not valid, id: "+split[0]);
            }
        }
    }
}
