package nl.mpcjanssen.simpletask.util;

import android.content.Context;
import nl.mpcjanssen.simpletask.R;

public class DateStrings {
    public String one_year_ago;
    public String years_ago;
    public String one_month_ago;
    public String months_ago;
    public String one_week_ago;
    public String weeks_ago;
    public String one_day_ago;
    public String days_ago;
    public String today;

    public DateStrings(Context context, int period) {
        int months = period/31;
        int weeks = period/7;
        int years = period/365;
        if (context!=null) {
            this.one_year_ago = context.getString(R.string.dates_one_year_ago);
            this.years_ago = context.getString(R.string.dates_years_ago, years);
            this.one_month_ago = context.getString(R.string.dates_one_month_ago);
            this.months_ago = context.getString(R.string.dates_months_ago, months);
            this.one_week_ago =context.getString(R.string.dates_one_week_ago);
            this.weeks_ago = context.getString(R.string.dates_weeks_ago, weeks);
            this.one_day_ago = context.getString(R.string.dates_one_day_ago);
            this.days_ago =  context.getString(R.string.dates_days_ago, period);
            this.today  = context.getString(R.string.dates_today);
        } else {

            this.one_year_ago = "1 year ago";
            this.years_ago = "" + years + " years ago";
            this.one_month_ago = "1 month ago";
            this.months_ago = "" + months + "months ago";
            this.one_week_ago = "1 week ago";
            this.weeks_ago = "" + weeks + " weeks ago";
            this.one_day_ago = "1 day ago";
            this.days_ago =  "" + period + " days ago";
            this.today  = "today";
        }
    }
}
