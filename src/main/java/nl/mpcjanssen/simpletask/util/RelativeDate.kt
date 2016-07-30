/**
 * This file is part of Todo.txt Touch, an Android app for managing your todo.txt file (http://todotxt.com).

 * Thanks to: http://kurtischiappone.com/programming/java/relative-date
 */
package nl.mpcjanssen.simpletask.util

import android.content.Context
import hirondelle.date4j.DateTime
import nl.mpcjanssen.simpletask.Constants
import nl.mpcjanssen.simpletask.R
import java.util.*


object RelativeDate {

    /**
     * This method computes the relative date according to the Calendar being
     * passed in and the number of years, months, days, etc. that differ. This
     * will compute both past and future relative dates. E.g., "one day ago" and
     * "one day from now".amount
     *
     *
     * **NOTE:** If the calendar date relative to "now" is older
     * than one day, we display the actual date in its default format as
     * specified by this class. If you don't want to
     * show the actual date, but you want to show the relative date for days,
     * months, and years, you can add the other cases in by copying the logic
     * for hours, minutes, seconds.

     * @param now current date
     * *
     * @param when date to calculate difference to
     * *
     * @return String representing the relative date
     */

    fun computeRelativeDate(context: Context?, now: DateTime, `when`: DateTime): String {
        if (context==null) {
            return now.format(Constants.DATE_FORMAT)
        }
        if (`when`.lteq(now)) {
            val period = `when`.numDaysFrom(now)

            val months = period / 31
            val weeks = period / 7
            val years = period / 365

            if (years == 1) {
                return context.getString(R.string.dates_one_year_ago)
            } else if (years > 1) {
                return context.getString(R.string.dates_years_ago, years)
            }
            if (months == 1) {
                return context.getString(R.string.dates_one_month_ago)
            } else if (months > 1) {
                return context.getString(R.string.dates_months_ago, months)
            }
            if (weeks == 1) {
                return context.getString(R.string.dates_one_week_ago)
            } else if (weeks > 1) {
                return context.getString(R.string.dates_weeks_ago, weeks)
            }
            if (period == 1) {
                return context.getString(R.string.dates_one_day_ago)
            } else if (period > 1) {
                return context.getString(R.string.dates_days_ago, period)
            } else if (period == 0) {
                return context.getString(R.string.dates_today)
            }
        }
        return `when`.toString()
    }

    /**
     * This method returns a String representing the relative date by comparing
     * the Calendar being passed in to the date / time that it is right now.

     * @param when date to calculate difference to
     * *
     * @return String representing the relative date
     */

    fun getRelativeDate(context: Context, `when`: DateTime): String {
        val now = DateTime.today(TimeZone.getDefault())
        return computeRelativeDate(context, now, `when`)

    }


}
