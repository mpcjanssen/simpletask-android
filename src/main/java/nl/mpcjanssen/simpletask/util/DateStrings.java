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

    public DateStrings(Context context) {
        this.one_year_ago = context.getString(R.string.dates_one_year_ago);
        this.years_ago = context.getString(R.string.dates_years_ago, "");
        this.one_month_ago = context.getString(R.string.dates_one_month_ago);
        this.months_ago = context.getString(R.string.dates_months_ago, "");
        this.one_week_ago =context.getString(R.string.dates_one_week_ago);
        this.weeks_ago = context.getString(R.string.dates_weeks_ago, "");
        this.one_day_ago = context.getString(R.string.dates_one_day_ago);
        this.days_ago =  context.getString(R.string.dates_days_ago, "");
        this.today  = context.getString(R.string.dates_today);
    }

    public DateStrings () {
        this.one_year_ago = " year ago";
        this.years_ago = " years ago";
        this.one_month_ago =" month ago";
        this.months_ago = " months ago";
        this.one_week_ago =" week ago";
        this.weeks_ago = " weeks ago";
        this.one_day_ago = " day ago";
        this.days_ago =  " days ago";
        this.today  = "today";
    }

}
