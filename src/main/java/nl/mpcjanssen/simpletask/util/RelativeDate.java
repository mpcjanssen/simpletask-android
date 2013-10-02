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
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.joda.time.Weeks;
import org.joda.time.Years;

import nl.mpcjanssen.simpletask.TodoApplication;
import nl.mpcjanssen.simpletask.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class RelativeDate {

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	private static Context context = TodoApplication.getAppContext();
	
	
	/**
	 * This method computes the relative date according to the Calendar being
	 * passed in and the number of years, months, days, etc. that differ. This
	 * will compute both past and future relative dates. E.g., "one day ago" and
	 * "one day from now".
	 * <p>
	 * <strong>NOTE:</strong> If the calendar date relative to "now" is older
	 * than one day, we display the actual date in its default format as
	 * specified by this class. The date format may be changed by calling
	 * {@link RelativeDate#setDateFormat(SimpleDateFormat)} If you don't want to
	 * show the actual date, but you want to show the relative date for days,
	 * months, and years, you can add the other cases in by copying the logic
	 * for hours, minutes, seconds.
	 * 
	 * @param now
	 * @param when
	 * @return String representing the relative date
	 */

    public static String computeRelativeDate(Calendar now, Calendar when) {

        String date = sdf.format(now.getTime());

        LocalDate start = LocalDate.fromCalendarFields(now);
        LocalDate end = LocalDate.fromCalendarFields(when);

        int days = Days.daysBetween(start, end).getDays();
        int months = Months.monthsBetween(start, end).getMonths();
        int weeks = Weeks.weeksBetween(start, end).getWeeks();
        int years = Years.yearsBetween(start, end).getYears();

        if (days > 0) {
            return date;
        }
        if (days == 0) {
            return context.getString(R.string.dates_today);
        }

        if (years == -1) {
            return context.getString(R.string.dates_one_year_ago);
        } else if (years < -1) {
            return context.getString(R.string.dates_years_ago, Math.abs(years) );
        }
        if (months == -1) {
            return context.getString(R.string.dates_one_month_ago);
        } else if (months < -1) {
            return context.getString(R.string.dates_months_ago, Math.abs(months) );
        }
        if (weeks == -1) {
            return context.getString(R.string.dates_one_week_ago);
        } else if (weeks < -1) {
            return context.getString(R.string.dates_weeks_ago, Math.abs(weeks) );
        }
        if (days == -1) {
            return context.getString(R.string.dates_one_day_ago);
        } else if (days < -1) {
            return context.getString(R.string.dates_days_ago, Math.abs(days));
        }
        return date;
    }

    /**
	 * This method returns a String representing the relative date by comparing
	 * the Calendar being passed in to the date / time that it is right now.
	 * 
	 * @param calendar
	 * @return String representing the relative date
	 */

	public static String getRelativeDate(Calendar calendar) {

		Calendar now = Calendar.getInstance();
		return computeRelativeDate(now, calendar);

	}

	/**
	 * This method returns a String representing the relative date by comparing
	 * the Date being passed in to the date / time that it is right now.
	 * Future dates will be returned as is.
	 * @param date
	 * @return String representing the relative date
	 */

	public static String getRelativeDate(Date date) {
        if (date.after(new Date())) {
           return sdf.format(date);
        }
		Calendar converted = Calendar.getInstance();
		converted.setTime(date);
		return getRelativeDate(converted);
	}

	/**
	 * This method sets the date format. This is used when the relative date is
	 * beyond one day. E.g., if the relative date is > 1 day, we will display
	 * the date in the format: h:mm a MMM dd, yyyy
	 * <p>
	 * This can be changed by passing in a new simple date format and then
	 * calling {@link RelativeDate#getRelativeDate(Calendar)} or
	 * {@link RelativeDate#getRelativeDate(Date)}.
	 * 
	 * @param dateFormat
	 */

	public static void setDateFormat(SimpleDateFormat dateFormat) {
		sdf = dateFormat;
	}

}