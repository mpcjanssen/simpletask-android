/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).
 * 
 * Thanks to: http://kurtischiappone.com/programming/java/relative-date
 */
package nl.mpcjanssen.simpletask.util;

import android.content.Context;

import java.util.TimeZone;

import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.R;
import nl.mpcjanssen.simpletask.TodoApplication;


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
	 * @param now current date
	 * @param when date to calculate difference to
	 * @return String representing the relative date
	 */

    public static String computeRelativeDate(DateTime now, DateTime when) {
        if (when.lteq(now)) {
            int period = when.numDaysFrom(now);

            int months = period/31;
            int weeks = period/7;
            int years = period/365;

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
            if (period == 1) {
                return context.getString(R.string.dates_one_day_ago);
            } else if (period > 1) {
                return context.getString(R.string.dates_days_ago, Math.abs(period));
            } else if (period == 0) {
                return context.getString(R.string.dates_today);
            }
        }
        return when.toString();
    }

    /**
	 * This method returns a String representing the relative date by comparing
	 * the Calendar being passed in to the date / time that it is right now.
	 * 
	 * @param when date to calculate difference to
	 * @return String representing the relative date
	 */

	public static String getRelativeDate(DateTime when) {

		DateTime now = DateTime.today(TimeZone.getDefault());
		return computeRelativeDate(now, when);

	}


}