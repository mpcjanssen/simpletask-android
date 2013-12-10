/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 * 
 * Thanks to: http://kurtischiappone.com/programming/java/relative-date
 */
package nl.mpcjanssen.simpletask.util;

import android.content.Context;
import android.util.Log;

import org.joda.time.DateMidnight;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Period;
import org.joda.time.ReadableDuration;
import org.joda.time.Weeks;
import org.joda.time.Years;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class RelativeDate {

	private static Context context = TodoApplication.getAppContext();
	
	
	/**
	 * This method computes the relative date according to the Calendar being
	 * passed in and the number of years, months, days, etc. that differ. This
	 * will compute both past and future relative dates. E.g., "one day ago" and
	 * "one day from now".
	 * <p>
	 * <strong>NOTE:</strong> If the calendar date relative to "now" is older
	 * than one day, we display the actual date in its default format as
	 * specified by this class. If you don't want to
	 * show the actual date, but you want to show the relative date for days,
	 * months, and years, you can add the other cases in by copying the logic
	 * for hours, minutes, seconds.
	 * 
	 * @param now
	 * @param when
	 * @return String representing the relative date
	 */

    public static String computeRelativeDate(DateTime now, DateTime when) {
        if (when.isBefore(now) || when.equals(now)) {
            Interval interval = new Interval(when, now);
            Period period = interval.toPeriod();

            int months = period.getMonths();
            int weeks = period.getWeeks();
            int years = period.getYears();
            int days = period.getDays();

            if (years == 1) {
                return context.getString(R.string.dates_one_year_ago);
            } else if (years > 1) {
                return context.getString(R.string.dates_years_ago, Math.abs(years));
            }
            if (months == 1) {
                return context.getString(R.string.dates_one_month_ago);
            } else if (months > 1) {
                return context.getString(R.string.dates_months_ago, Math.abs(months));
            }
            if (weeks == 1) {
                return context.getString(R.string.dates_one_week_ago);
            } else if (weeks > 1) {
                return context.getString(R.string.dates_weeks_ago, Math.abs(weeks));
            }
            if (days == 1) {
                return context.getString(R.string.dates_one_day_ago);
            } else if (days > 1) {
                return context.getString(R.string.dates_days_ago, Math.abs(days));
            } else if (days == 0) {
                return context.getString(R.string.dates_today);
            }
        }
        return when.toString(ISODateTimeFormat.date());
    }

    /**
	 * This method returns a String representing the relative date by comparing
	 * the Calendar being passed in to the date / time that it is right now.
	 * 
	 * @param when
	 * @return String representing the relative date
	 */

	public static String getRelativeDate(DateTime when) {

		DateTime now = new DateTime();
		return computeRelativeDate(now, when);

	}


}