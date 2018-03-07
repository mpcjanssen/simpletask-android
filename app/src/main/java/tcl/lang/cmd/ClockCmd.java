/*
 * ClockCmd.java --
 *
 *	Implements the built-in "clock" Tcl command.
 *
 * Copyright (c) 1998-2000 Christian Krone.
 * Copyright (c) 1997 Cornell University.
 * Copyright (c) 1995-1997 Sun Microsystems, Inc.
 * Copyright (c) 1992-1995 Karl Lehenbauer and Mark Diekhans.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 * 
 * RCS: @(#) $Id: ClockCmd.java,v 1.9 2009/06/22 17:05:08 rszulgo Exp $
 *
 */

package tcl.lang.cmd;

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TCL;
import tcl.lang.TclBoolean;
import tcl.lang.TclException;
import tcl.lang.TclIndex;
import tcl.lang.TclInteger;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * This class implements the built-in "clock" command in Tcl.
 */

public class ClockCmd implements Command {

	static final private String[] validCmds = { "clicks", "format", "scan", "seconds" };

	static final private int CMD_CLICKS = 0;
	static final private int CMD_FORMAT = 1;
	static final private int CMD_SCAN = 2;
	static final private int CMD_SECONDS = 3;

	static final private String clicksOpts[] = { "-milliseconds" };

	static final private int OPT_CLICKS_MILLISECONDS = 0;

	static final private String[] formatOpts = { "-format", "-gmt" };

	static final private int OPT_FORMAT_FORMAT = 0;
	static final private int OPT_FORMAT_GMT = 1;

	static final private String[] scanOpts = { "-base", "-gmt" };

	static final private int OPT_SCAN_BASE = 0;
	static final private int OPT_SCAN_GMT = 1;

	static final int EPOCH_YEAR = 1970;
	static final int MILLIS_PER_HOUR = 60 * 60 * 1000;

	/**
	 *----------------------------------------------------------------------
	 * 
	 * cmdProc --
	 * 
	 * This procedure is invoked as part of the Command interface to process the
	 * "clock" Tcl command. See the user documentation for details on what it
	 * does.
	 * 
	 * Results: None.
	 * 
	 * Side effects: See the user documentation.
	 * 
	 *----------------------------------------------------------------------
	 */

	public void cmdProc(Interp interp, // Current interpreter.
			TclObject[] objv) // Argument list.
			throws TclException // A standard Tcl exception.
	{
		int clockVal; // Time value as seconds of epoch.
		String dateString; // Time value as string.
		int argIx; // Counter over arguments.
		String format = null; // User specified format string.
		boolean useGmt = false; // User specified flag to use gmt.
		TclObject baseObj = null; // User specified raw value of baseClock.
		Date baseClock; // User specified time value.
		Date date; // Parsed date value.

		if (objv.length < 2) {
			throw new TclNumArgsException(interp, 1, objv, "option ?arg ...?");
		}
		int cmd = TclIndex.get(interp, objv[1], validCmds, "option", 0);

		switch (cmd) {
		case CMD_CLICKS: {
			if (objv.length > 3) {
				throw new TclNumArgsException(interp, 2, objv, "?-milliseconds?");
			}
			if (objv.length == 3) {
				// We can safely ignore the -milliseconds options, since
				// we measure the clicks in milliseconds anyway...
				TclIndex.get(interp, objv[2], clicksOpts, "switch", 0);
				if (objv[2].toString().equals("-")) {
					/* Special case - can't abbreviate this much */
					throw new TclException(interp, "bad switch \"-\": must be -milliseconds");
				}
			}
			interp.setResult(System.currentTimeMillis());
			break;
		}

		case CMD_FORMAT: {
			if ((objv.length < 3) || (objv.length > 7)) {
				throw new TclNumArgsException(interp, 2, objv, "clockval ?-format string? ?-gmt boolean?");
			}
			clockVal = TclInteger.getInt(interp, objv[2]);

			for (argIx = 3; argIx + 1 < objv.length; argIx += 2) {
				int formatOpt = TclIndex.get(interp, objv[argIx], formatOpts, "switch", 0);
				switch (formatOpt) {
				case OPT_FORMAT_FORMAT: {
					format = objv[argIx + 1].toString();
					break;
				}
				case OPT_FORMAT_GMT: {
					useGmt = TclBoolean.get(interp, objv[argIx + 1]);
					break;
				}
				}
			}
			if (argIx < objv.length) {
				throw new TclNumArgsException(interp, 2, objv, "clockval ?-format string? ?-gmt boolean?");
			}
			FormatClock(interp, clockVal, useGmt, format);
			break;
		}

		case CMD_SCAN: {
			if ((objv.length < 3) || (objv.length > 7)) {
				throw new TclNumArgsException(interp, 2, objv, "dateString ?-base clockValue? ?-gmt boolean?");
			}
			dateString = objv[2].toString();
			
			// check for empty datestring
			if (dateString.trim().length() == 0) {
				long millis = System.currentTimeMillis();
				int seconds = (int) (millis / 1000);
				interp.setResult(seconds);
				break;
			}

			for (argIx = 3; argIx + 1 < objv.length; argIx += 2) {
				int scanOpt = TclIndex.get(interp, objv[argIx], scanOpts, "switch", 0);
				switch (scanOpt) {
				case OPT_SCAN_BASE: {
					baseObj = objv[argIx + 1];
					break;
				}
				case OPT_SCAN_GMT: {
					useGmt = TclBoolean.get(interp, objv[argIx + 1]);
					break;
				}
				}
			}
			if (argIx < objv.length) {
				throw new TclNumArgsException(interp, 2, objv, "clockval ?-format string? ?-gmt boolean?");
			}
			if (baseObj != null) {
				long seconds = TclInteger.getLong(interp, baseObj);
				baseClock = new Date(seconds * 1000);
			} else {
				baseClock = new Date();
			}

			date = GetDate(dateString, baseClock, useGmt);
			if (date == null) {
				throw new TclException(interp, "unable to convert date-time string \"" + dateString + "\"");
			}

			int seconds = (int) (date.getTime() / 1000);
			interp.setResult(seconds);
			break;
		}

		case CMD_SECONDS: {
			if (objv.length != 2) {
				throw new TclNumArgsException(interp, 2, objv, null);
			}
			long millis = System.currentTimeMillis();
			int seconds = (int) (millis / 1000);
			interp.setResult(seconds);
			break;
		}
		}
	}

	/**
	 * Formats a time value based on seconds into a human readable string.
	 * Formatted string is returned in the interpreter's result
	 * 
	 * @param interp
	 *            interpreter into which to put result
	 * @param clockVal
	 *            Time in seconds.
	 * @param useGMT
	 *            true if use GMT, if false use environment's defined timezone
	 * @param format
	 *            format string
	 * @throws TclException
	 */
	private void FormatClock(Interp interp, int clockVal, boolean useGMT, String format) throws TclException {
		Date date = new Date((long) clockVal * 1000);
		GregorianCalendar calendar = new GregorianCalendar();
		SimpleDateFormat fmt, locFmt;
		FieldPosition fp = new FieldPosition(0);
		StringBuffer result = new StringBuffer();

		if (format == null) {
			format = new String("%a %b %d %H:%M:%S %Z %Y");
		}

		if (useGMT) {
			calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		} else {
			try {
				TclObject tz = interp.getVar("env", "TZ", TCL.GLOBAL_ONLY);
				calendar.setTimeZone(TimeZone.getTimeZone(tz.toString()));
			} catch (TclException e) {
			}
		}
		calendar.setTime(date);
		fmt = new SimpleDateFormat("mm.dd.yy", Locale.US);
		fmt.setCalendar(calendar);

		if (format.equals("%Q")) { // Enterprise Stardate.
			int trekYear = calendar.get(Calendar.YEAR) + 377 - 2323;
			int trekDay = (calendar.get(Calendar.DAY_OF_YEAR) * 1000)
					/ (calendar.isLeapYear(calendar.get(Calendar.YEAR)) ? 366 : 365);
			int trekHour = (calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)) / 144;

			interp.setResult("Stardate " + (trekYear < 10 ? "0" : "") + (trekYear * 1000 + trekDay) + '.' + trekHour);
			return;
		}

		for (int ix = 0; ix < format.length(); ix++) {
			if (format.charAt(ix) == '%' && ix + 1 < format.length()) {
				switch (format.charAt(++ix)) {
				case '%': // Insert a %.
					result.append('%');
					break;
				case 'a': // Abbreviated weekday name (Mon, Tue, etc.).
					fmt.applyPattern("EEE");
					fmt.format(date, result, fp);
					break;
				case 'A': // Full weekday name (Monday, Tuesday, etc.).
					fmt.applyPattern("EEEE");
					fmt.format(date, result, fp);
					break;
				case 'b':
					/* falls through */
				case 'h': // Abbreviated month name (Jan,Feb,etc.).
					fmt.applyPattern("MMM");
					fmt.format(date, result, fp);
					break;
				case 'B': // Full month name.
					fmt.applyPattern("MMMM");
					fmt.format(date, result, fp);
					break;
				case 'c': // Locale specific date and time.
					locFmt = (SimpleDateFormat) DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
					locFmt.setCalendar(calendar);
					locFmt.format(date, result, fp);
					break;
				case 'C': // Century (00 - 99).
					int century = calendar.get(Calendar.YEAR) / 100;
					result.append((century < 10 ? "0" : "") + century);
					break;
				case 'd': // Day of month (01 - 31).
					fmt.applyPattern("dd");
					fmt.format(date, result, fp);
					break;
				case 'D': // Date as %m/%d/%y.
					fmt.applyPattern("MM/dd/yy");
					fmt.format(date, result, fp);
					break;
				case 'e': // Day of month (1 - 31), no leading zeros.
					fmt.applyPattern("d");
					String day = fmt.format(date);
					result.append((day.length() < 2 ? " " : "") + day);
					break;
				case 'g': // The ISO8601 year number corresponding to the
					// ISO8601 week (%V),
					// expressed as a two-digit year-of-the-century, with
					// leading zero if necessary.
					int isoYear = getISO8601Year(calendar);
					result.append((isoYear % 1000 < 10 ? "0" : "") + isoYear % 1000);
					break;
				case 'G': // The ISO8601 year number corresponding to the
					// ISO8601 week (%V),
					// expressed as a four-digit number.
					isoYear = getISO8601Year(calendar);
					if (isoYear < 1000) {
						result.append("0");
					} else if (isoYear < 100) {
						result.append("00");
					} else if (isoYear < 10) {
						result.append("000");
					}

					result.append(isoYear);

					break;
				case 'H': // Hour in 24-hour format (00 - 23).
					fmt.applyPattern("HH");
					fmt.format(date, result, fp);
					break;
				case 'I': // Hour in 12-hour format (01 - 12).
					fmt.applyPattern("hh");
					fmt.format(date, result, fp);
					break;
				case 'j': // Day of year (001 - 366).
					fmt.applyPattern("DDD");
					fmt.format(date, result, fp);
					break;
				case 'k': // Hour in 24-hour format (0 - 23), no leading zeros.
					fmt.applyPattern("H");
					String h24 = fmt.format(date);
					result.append((h24.length() < 2 ? " " : "") + h24);
					break;
				case 'l': // Hour in 12-hour format (1 - 12), no leading zeros.
					fmt.applyPattern("h");
					String h12 = fmt.format(date);
					result.append((h12.length() < 2 ? " " : "") + h12);
					break;
				case 'm': // Month number (01 - 12).
					fmt.applyPattern("MM");
					fmt.format(date, result, fp);
					break;
				case 'M': // Minute (00 - 59).
					fmt.applyPattern("mm");
					fmt.format(date, result, fp);
					break;
				case 'n': // Insert a newline.
					result.append('\n');
					break;
				case 'p': // AM/PM indicator.
					fmt.applyPattern("aa");
					fmt.format(date, result, fp);
					break;
				case 'r': // Time as %I:%M:%S %p.
					fmt.applyPattern("KK:mm:ss aaaa");
					fmt.format(date, result, fp);
					break;
				case 'R': // Time as %H:%M.
					fmt.applyPattern("hh:mm");
					fmt.format(date, result, fp);
					break;
				case 's': // seconds since epoch.
					long millis = calendar.getTime().getTime();
					if (useGMT) {
						Calendar localCalendar = Calendar.getInstance();
						localCalendar.setTime(calendar.getTime());
						millis -= localCalendar.get(Calendar.ZONE_OFFSET) + localCalendar.get(Calendar.DST_OFFSET);
					}
					result.append((int) (millis / 1000));
					break;
				case 'S': // Seconds (00 - 59).
					fmt.applyPattern("ss");
					fmt.format(date, result, fp);
					break;
				case 't': // Insert a tab.
					result.append('\t');
					break;
				case 'T': // Time as %H:%M:%S.
					fmt.applyPattern("hh:mm:ss");
					fmt.format(date, result, fp);
					break;
				case 'u': // Weekday number (1 - 7) Sunday = 7.
					int dayOfWeek17 = calendar.get(Calendar.DAY_OF_WEEK);
					if (dayOfWeek17 == Calendar.SUNDAY) {
						result.append(7);
					} else {
						result.append(dayOfWeek17 - Calendar.SUNDAY);
					}
					break;
				case 'U': // Week of year (01-52), Sunday is first day.
					int weekS = GetWeek(calendar, Calendar.SUNDAY, false);
					result.append((weekS < 10 ? "0" : "") + weekS);
					break;
				case 'V': // ISO 8601 Week Of Year (01 - 53).
					int isoWeek = GetWeek(calendar, Calendar.MONDAY, true);
					result.append((isoWeek < 10 ? "0" : "") + isoWeek);
					break;
				case 'w': // Weekday number (0 - 6) Sunday = 0.
					int dayOfWeek06 = calendar.get(Calendar.DAY_OF_WEEK);
					result.append(dayOfWeek06 - Calendar.SUNDAY);
					break;
				case 'W': // Week of year (01-52), Monday is first day.
					int weekM = GetWeek(calendar, Calendar.MONDAY, false);
					result.append((weekM < 10 ? "0" : "") + weekM);
					break;
				case 'x': // Locale specific date format.
					locFmt = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT);
					locFmt.setCalendar(calendar);
					locFmt.format(date, result, fp);
					break;
				case 'X': // Locale specific time format.
					locFmt = (SimpleDateFormat) DateFormat.getTimeInstance(DateFormat.SHORT);
					locFmt.setCalendar(calendar);
					locFmt.format(date, result, fp);
					break;
				case 'y': // Year without century (00 - 99).
					fmt.applyPattern("yy");
					fmt.format(date, result, fp);
					break;
				case 'Y': // Year with century (e.g. 1990)
					fmt.applyPattern("yyyy");
					fmt.format(date, result, fp);
					break;
				case 'Z': // Time zone name.
					fmt.applyPattern("zzz");
					fmt.format(date, result, fp);
					break;
				default:
					result.append(format.charAt(ix));
					break;
				}
			} else {
				result.append(format.charAt(ix));
			}
		}
		interp.setResult(result.toString());
	}

	/**
	 * Calculate the correct year for an ISO8601 week
	 * 
	 * @param calendar
	 *            calendar with given date
	 * @return year
	 */
	private int getISO8601Year(Calendar calendar) {
		int fdow = calendar.getFirstDayOfWeek();
		int mdfw = calendar.getMinimalDaysInFirstWeek();
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		calendar.setMinimalDaysInFirstWeek(4);
		int week = calendar.get(Calendar.WEEK_OF_YEAR);
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		calendar.setFirstDayOfWeek(fdow);
		calendar.setMinimalDaysInFirstWeek(mdfw);
		calendar.setTime(calendar.getTime());
		if (week == 1 && month == Calendar.DECEMBER) {
			return year + 1;
		}
		if ((week == 52 || week == 53) && month == Calendar.JANUARY) {
			return year - 1;
		}
		return year;
	}

	/**
	 * Returns the week_of_year of the given date. The weekday considered as
	 * start of the week is given as argument. Specify iso as true to get the
	 * week_of_year accourding to ISO.
	 * 
	 * Side effects: The interpreter will contain the formatted string as
	 * result.
	 * 
	 * @param calendar
	 *            calendar containing the date
	 * @param firstDayOfWeek
	 *            this day starts a week
	 * @param boolean iso if true, evaluate according to ISO8601
	 * @return Day of the week .
	 */
	private int GetWeek(Calendar calendar, // Calendar containing Date.
			int firstDayOfWeek, // this day starts a week (MONDAY/SUNDAY).
			boolean iso) // evaluate according to ISO?
	{
		if (iso) {
			firstDayOfWeek = Calendar.MONDAY;
		}

		// After changing the firstDayOfWeek, we have to set the time value
		// anew,
		// so that the fields of the calendar are recalculated.
		int fdow = calendar.getFirstDayOfWeek();
		int mdfw = calendar.getMinimalDaysInFirstWeek();
		calendar.setFirstDayOfWeek(firstDayOfWeek);
		calendar.setMinimalDaysInFirstWeek(iso ? 4 : 7);
		calendar.setTime(calendar.getTime());
		int week = calendar.get(Calendar.WEEK_OF_YEAR);

		if (!iso) {
			// The week for the first days of the year may be 52 or 53.
			// But here we have to return 0, if we don't compute ISO week.
			// So any bigger than 50th week in January will become 00.

			if (calendar.get(Calendar.MONTH) == Calendar.JANUARY && week > 50) {
				week = 0;
			}
		}
		calendar.setFirstDayOfWeek(fdow);
		calendar.setMinimalDaysInFirstWeek(mdfw);
		return week;
	}

	/**
	 * The date of the given calendar will be incremented, so that it will match
	 * the weekday in the diff object. If dayOrdinal is bigger than 1,
	 * additional weeks will be added.
	 * 
	 * @param calendar
	 *            contains date
	 * @param diff
	 *            time difference to evaluate
	 */

	private void SetWeekday(Calendar calendar, // Calendar containing Date.
			ClockRelTimespan diff) // time difference to evaluate
	{
		int weekday = diff.getWeekday();
		int dayOrdinal = diff.getDayOrdinal();

		while (calendar.get(Calendar.DAY_OF_WEEK) != weekday) {
			calendar.add(Calendar.DATE, 1);
		}
		if (dayOrdinal > 1) {
			calendar.add(Calendar.DATE, 7 * (dayOrdinal - 1));
		}
	}

	/**
	 * The date of the given calendar will be incremented, so that it will match
	 * the ordinal month in the diff object.
	 * 
	 * Results: None.
	 * 
	 * Side effects: Modifies the given calendar.
	 */

	private void SetOrdMonth(Calendar calendar, // Calendar containing Date.
			ClockRelTimespan diff) // time difference to evaluate
	{
		int month = diff.getMonths();
		int ordMonth = diff.getOrdMonth();

		calendar.add(Calendar.MONTH, 1); /* we want to get the next month... */
		while (calendar.get(Calendar.MONTH) != month) {
			calendar.add(Calendar.MONTH, 1);
		}
		if (ordMonth > 1) {
			calendar.add(Calendar.YEAR, ordMonth - 1);
		}
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		calendar.clear(Calendar.HOUR_OF_DAY);
		calendar.clear(Calendar.MINUTE);
		calendar.clear(Calendar.SECOND);
	}

	/**
	 * Scan a human readable date string and construct a Date.
	 * 
	 * Results: The scanned date (or null, if an error occured).
	 * 
	 * Side effects: None.
	 */

	private Date GetDate(String dateString, // Date string to scan
			Date baseDate, // Date to use as base
			boolean useGMT) // Boolean
	{
		GregorianCalendar calendar = new GregorianCalendar();
		Calendar now = Calendar.getInstance();
		now.setTime(baseDate);
		calendar.set(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		if (useGMT) {
			calendar.setTimeZone(TimeZone.getTimeZone("GMT"));
		}

		ClockToken[] dt = GetTokens(dateString, false);

		ParsePosition parsePos = new ParsePosition(0);
		ClockRelTimespan diff = new ClockRelTimespan();
		int hasTime = 0;
		int hasZone = 0;
		int hasDate = 0;
		int hasDay = 0;
		int hasOrdMonth = 0;
		int hasRel = 0;

		while (parsePos.getIndex() < dt.length) {
			if (ParseTime(dt, parsePos, calendar)) {
				hasTime++;
			} else if (ParseZone(dt, parsePos, calendar)) {
				hasZone++;
			} else if (ParseIso(dt, parsePos, calendar)) {
				hasDate++;
			} else if (ParseDate(dt, parsePos, calendar)) {
				hasDate++;
			} else if (ParseDay(dt, parsePos, diff)) {
				hasDay++;
			} else if (ParseOrdMonth(dt, parsePos, diff)) {
				hasOrdMonth++;
			} else if (ParseRelSpec(dt, parsePos, diff)) {
				hasRel++;
			} else if (ParseNumber(dt, parsePos, calendar, hasDate > 0 && hasTime > 0 && hasRel == 0)) {
				if (hasDate == 0 || hasTime == 0 || hasRel > 0) {
					hasTime++;
				}
			} else if (ParseTrek(dt, parsePos, calendar)) {
				hasDate++;
				hasTime++;
			} else {
				return null;
			}
		}

		if (hasTime > 1 || hasZone > 1 || hasDate > 1 || hasDay > 1 || hasOrdMonth > 1) {
			return null;
		}

		// The following line handles years that are specified using
		// only two digits. The line of code below implements a policy
		// defined by the X/Open workgroup on the millinium rollover.
		// Note: some of those dates may not actually be valid on some
		// platforms. The POSIX standard startes that the dates 70-99
		// shall refer to 1970-1999 and 00-38 shall refer to 2000-2038.
		// This later definition should work on all platforms.

		int thisYear = calendar.get(Calendar.YEAR);
		if (thisYear < 100) {
			if (thisYear >= 69) {
				calendar.set(Calendar.YEAR, thisYear + 1900);
			} else {
				calendar.set(Calendar.YEAR, thisYear + 2000);
			}
		}

		if (hasRel > 0) {
			if (hasTime == 0 && hasDate == 0 && hasDay == 0) {
				calendar.setTime(baseDate);
			}
			// Certain JDK implementations are buggy WRT DST.
			// Work around this issue by adding a day instead
			// of a days worth of seconds.
			final int seconds_in_day = (60 * 60 * 24);
			int seconds = diff.getSeconds();
			boolean negative_seconds = (seconds < 0);
			int days = 0;
			if (negative_seconds)
				seconds *= -1;
			while (seconds >= seconds_in_day) {
				seconds -= seconds_in_day;
				days++;
			}
			if (negative_seconds) {
				seconds *= -1;
				days *= -1;
			}
			if (days != 0) {
				calendar.add(Calendar.DATE, days);
			}
			if (seconds != 0) {
				calendar.add(Calendar.SECOND, seconds);
			}
			calendar.add(Calendar.MONTH, diff.getMonths());
		}

		if (hasDay > 0 && hasDate == 0) {
			SetWeekday(calendar, diff);
		}

		if (hasOrdMonth > 0) {
			SetOrdMonth(calendar, diff);
		}

		return calendar.getTime();
	}

	/**
	 * Parse a time string and sets the Calendar. A time string is valid, if it
	 * confirms to the following yacc rule: time : tUNUMBER tMERIDIAN | tUNUMBER
	 * ':' tUNUMBER o_merid | tUNUMBER ':' tUNUMBER '-' tUNUMBER | tUNUMBER ':'
	 * tUNUMBER ':' tUNUMBER o_merid | tUNUMBER ':' tUNUMBER ':' tUNUMBER '-'
	 * tUNUMBER ;
	 * 
	 * Results: True, if a time was read (parsePos was incremented and calendar
	 * was set according to the read time); false otherwise.
	 * 
	 * Side effects: None.
	 */

	private boolean ParseTime(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			Calendar calendar) // calendar object to set
	{
		int pos = parsePos.getIndex();

		if (pos + 6 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is(':') && dt[pos + 2].isUNumber()
				&& dt[pos + 3].is(':') && dt[pos + 4].isUNumber() && dt[pos + 5].is('-') && dt[pos + 6].isUNumber()) {
			ClockToken zone = GetTimeZoneFromRawOffset(-dt[pos + 6].getInt() / 100);
			if (zone != null) {
				calendar.set(Calendar.HOUR_OF_DAY, dt[pos].getInt());
				calendar.set(Calendar.MINUTE, dt[pos + 2].getInt());
				calendar.set(Calendar.SECOND, dt[pos + 4].getInt());
				calendar.setTimeZone(zone.getZone());
				parsePos.setIndex(pos + 7);
				return true;
			}
		}
		if (pos + 4 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is(':') && dt[pos + 2].isUNumber()
				&& dt[pos + 3].is(':') && dt[pos + 4].isUNumber()) {
			parsePos.setIndex(pos + 5);
			ParseMeridianAndSetHour(dt, parsePos, calendar, dt[pos].getInt());
			calendar.set(Calendar.MINUTE, dt[pos + 2].getInt());
			calendar.set(Calendar.SECOND, dt[pos + 4].getInt());
			return true;
		}
		if (pos + 4 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is(':') && dt[pos + 2].isUNumber()
				&& dt[pos + 3].is('-') && dt[pos + 4].isUNumber()) {
			ClockToken zone = GetTimeZoneFromRawOffset(-dt[pos + 4].getInt() / 100);
			if (zone != null) {
				calendar.set(Calendar.HOUR_OF_DAY, dt[pos].getInt());
				calendar.set(Calendar.MINUTE, dt[pos + 2].getInt());
				calendar.setTimeZone(zone.getZone());
				parsePos.setIndex(pos + 5);
				return true;
			}
		}
		if (pos + 2 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is(':') && dt[pos + 2].isUNumber()) {
			parsePos.setIndex(pos + 3);
			ParseMeridianAndSetHour(dt, parsePos, calendar, dt[pos].getInt());
			calendar.set(Calendar.MINUTE, dt[pos + 2].getInt());
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is(ClockToken.MERIDIAN)) {
			parsePos.setIndex(pos + 1);
			ParseMeridianAndSetHour(dt, parsePos, calendar, dt[pos].getInt());
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseZone --
	 * 
	 * Parse a timezone string and sets the Calendar. A timezone string is
	 * valid, if it confirms to the following yacc rule: zone : tZONE tDST |
	 * tZONE | tDAYZONE ;
	 * 
	 * Results: True, if a timezone was read (parsePos was incremented and
	 * calendar was set according to the read timezone); false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseZone(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			Calendar calendar) // calendar object to set
	{
		int pos = parsePos.getIndex();

		if (pos + 1 < dt.length && dt[pos].is(ClockToken.ZONE) && dt[pos + 1].is(ClockToken.DST)) {
			calendar.setTimeZone(dt[pos].getZone());
			parsePos.setIndex(pos + 2);
			return true;
		}
		if (pos < dt.length && dt[pos].is(ClockToken.ZONE)) {
			calendar.setTimeZone(dt[pos].getZone());
			parsePos.setIndex(pos + 1);
			return true;
		}
		if (pos < dt.length && dt[pos].is(ClockToken.DAYZONE)) {
			calendar.setTimeZone(dt[pos].getZone());
			parsePos.setIndex(pos + 1);
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseDay --
	 * 
	 * Parse a day string and sets the Calendar. A day string is valid, if it
	 * confirms to the following yacc rule: day : tDAY | tDAY ',' | tUNUMBER
	 * tDAY | '+' tUNUMBER tDAY | '-' tUNUMBER tDAY | tNEXT tDAY ;
	 * 
	 * Results: True, if a day was read (parsePos was incremented and the time
	 * difference was set according to the read day); false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseDay(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			ClockRelTimespan diff) // time difference to evaluate
	{
		int pos = parsePos.getIndex();

		if (pos + 2 < dt.length && dt[pos].is('+') && dt[pos + 1].isUNumber() && dt[pos + 2].is(ClockToken.DAY)) {
			diff.setWeekday(dt[pos + 2].getInt(), dt[pos + 1].getInt());
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 2 < dt.length && dt[pos].is('-') && dt[pos + 1].isUNumber() && dt[pos + 2].is(ClockToken.DAY)) {
			diff.setWeekday(dt[pos + 2].getInt(), -dt[pos + 1].getInt());
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].is(ClockToken.NEXT) && dt[pos + 1].is(ClockToken.DAY)) {
			diff.setWeekday(dt[pos + 1].getInt(), 2);
			parsePos.setIndex(pos + 2);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].is(ClockToken.DAY) && dt[pos + 1].is(',')) {
			diff.setWeekday(dt[pos].getInt());
			parsePos.setIndex(pos + 2);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is(ClockToken.DAY)) {
			diff.setWeekday(dt[pos + 1].getInt(), dt[pos].getInt());
			parsePos.setIndex(pos + 2);
			return true;
		}
		if (pos < dt.length && dt[pos].is(ClockToken.DAY)) {
			diff.setWeekday(dt[pos].getInt());
			parsePos.setIndex(pos + 1);
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseDate --
	 * 
	 * Parse a date string and sets the Calendar. A date string is valid, if it
	 * confirms to the following yacc rule: date : tUNUMBER '/' tUNUMBER |
	 * tUNUMBER '/' tUNUMBER '/' tUNUMBER | tISOBASE | tUNUMBER '-' tMONTH '-'
	 * tUNUMBER | tUNUMBER '-' tUNUMBER '-' tUNUMBER | tMONTH tUNUMBER | tMONTH
	 * tUNUMBER ',' tUNUMBER | tUNUMBER tMONTH | tEPOCH | tUNUMBER tMONTH
	 * tUNUMBER ;
	 * 
	 * Results: True, if a date was read (parsePos was incremented and calendar
	 * was set according to the read day); false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseDate(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			Calendar calendar) // calendar object to set
	{
		int pos = parsePos.getIndex();

		if (pos + 4 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is('/') && dt[pos + 2].isUNumber()
				&& dt[pos + 3].is('/') && dt[pos + 4].isUNumber()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos + 2].getInt());
			calendar.set(Calendar.MONTH, dt[pos].getInt() - 1);
			calendar.set(Calendar.YEAR, dt[pos + 4].getInt());
			parsePos.setIndex(pos + 5);
			return true;
		}
		if (pos + 4 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is('-') && dt[pos + 2].is(ClockToken.MONTH)
				&& dt[pos + 3].is('-') && dt[pos + 4].isUNumber()) {
			calendar.set(Calendar.YEAR, dt[pos + 4].getInt());
			calendar.set(Calendar.MONTH, dt[pos + 2].getInt());
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt());
			parsePos.setIndex(pos + 5);
			return true;
		}
		if (pos + 4 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is('-') && dt[pos + 2].isUNumber()
				&& dt[pos + 3].is('-') && dt[pos + 4].isUNumber()) {
			calendar.set(Calendar.YEAR, dt[pos].getInt());
			calendar.set(Calendar.MONTH, dt[pos + 2].getInt() - 1);
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos + 4].getInt());
			parsePos.setIndex(pos + 5);
			return true;
		}
		if (pos + 3 < dt.length && dt[pos].is(ClockToken.MONTH) && dt[pos + 1].isUNumber() && dt[pos + 2].is(',')
				&& dt[pos + 3].isUNumber()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos + 1].getInt());
			calendar.set(Calendar.MONTH, dt[pos].getInt());
			calendar.set(Calendar.YEAR, dt[pos + 3].getInt());
			parsePos.setIndex(pos + 4);
			return true;
		}
		if (pos + 2 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is('/') && dt[pos + 2].isUNumber()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos + 2].getInt());
			calendar.set(Calendar.MONTH, dt[pos].getInt() - 1);
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 2 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is(ClockToken.MONTH) && dt[pos + 2].isUNumber()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt());
			calendar.set(Calendar.MONTH, dt[pos + 1].getInt());
			calendar.set(Calendar.YEAR, dt[pos + 2].getInt());
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].is(ClockToken.MONTH) && dt[pos + 1].isUNumber()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos + 1].getInt());
			calendar.set(Calendar.MONTH, dt[pos].getInt());
			parsePos.setIndex(pos + 2);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].isUNumber() && dt[pos + 1].is(ClockToken.MONTH)) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt());
			calendar.set(Calendar.MONTH, dt[pos + 1].getInt());
			parsePos.setIndex(pos + 2);
			return true;
		}
		if (pos < dt.length && dt[pos].isIsoBase()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt() % 100);
			calendar.set(Calendar.MONTH, (dt[pos].getInt() % 10000) / 100 - 1);
			calendar.set(Calendar.YEAR, dt[pos].getInt() / 10000);
			parsePos.setIndex(pos + 1);
			return true;
		}
		if (pos < dt.length && dt[pos].is(ClockToken.EPOCH)) {
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			calendar.set(Calendar.MONTH, 0);
			calendar.set(Calendar.YEAR, EPOCH_YEAR);
			parsePos.setIndex(pos + 1);
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseNumber --
	 * 
	 * Parse a number and sets the Calendar. If argument mayBeYear is true, this
	 * number is conidered as year, otherwise it is date and time in the form
	 * HHMM.
	 * 
	 * Results: True, if a number was read (parsePos was incremented and
	 * calendar was set according to the read day); false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseNumber(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			Calendar calendar, // calendar object to set
			boolean mayBeYear) // number is considered to be year?
	{
		int pos = parsePos.getIndex();

		if (pos < dt.length && dt[pos].isUNumber()) {
			parsePos.setIndex(pos + 1);
			if (mayBeYear) {
				calendar.set(Calendar.YEAR, dt[pos].getInt());
			} else {
				calendar.set(Calendar.HOUR_OF_DAY, dt[pos].getInt() / 100);
				calendar.set(Calendar.MINUTE, dt[pos].getInt() % 100);
				calendar.set(Calendar.SECOND, 0);
			}
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseRelSpec --
	 * 
	 * Parse a relative time specification and sets the time difference. A
	 * relative time specification is valid, if it confirms to the following
	 * yacc rule: relspec : relunits tAGO | relunits ;
	 * 
	 * Results: True, if a relative time specification was read (parsePos was
	 * incremented and the time difference was set according to the read
	 * relative time specification); false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseRelSpec(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			ClockRelTimespan diff) // time difference to evaluate
	{
		if (!ParseRelUnits(dt, parsePos, diff)) {
			return false;
		}

		int pos = parsePos.getIndex();
		if (pos < dt.length && dt[pos].is(ClockToken.AGO)) {
			diff.negate();
			parsePos.setIndex(pos + 1);
		}
		return true;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseRelUnits --
	 * 
	 * Parse a relative time unit and sets the time difference. A relative time
	 * unit is valid, if it confirms to the following yacc rule: relspec : '+'
	 * tUNUMBER unit | '-' tUNUMBER unit | tUNUMBER unit | tNEXT unit | tNEXT
	 * tUNUMBER unit | unit ;
	 * 
	 * Results: True, if a relative time specification was read (parsePos was
	 * incremented and the time difference was set according to the read
	 * relative time specification); false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseRelUnits(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			ClockRelTimespan diff) // time difference to evaluate
	{
		int pos = parsePos.getIndex();

		if (pos + 2 < dt.length && dt[pos].is('+') && dt[pos + 1].isUNumber() && dt[pos + 2].isUnit()) {
			diff.addUnit(dt[pos + 2], dt[pos + 1].getInt());
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 2 < dt.length && dt[pos].is('-') && dt[pos + 1].isUNumber() && dt[pos + 2].isUnit()) {
			diff.addUnit(dt[pos + 2], -dt[pos + 1].getInt());
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].isUNumber() && dt[pos + 1].isUnit()) {
			diff.addUnit(dt[pos + 1], dt[pos].getInt());
			parsePos.setIndex(pos + 2);
			return true;
		} else if (pos + 2 < dt.length && dt[pos].is(ClockToken.NEXT) && dt[pos + 1].isUNumber()
				&& dt[pos + 2].isUnit()) {
			diff.addUnit(dt[pos + 2], dt[pos + 1].getInt());
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].is(ClockToken.NEXT) && dt[pos + 1].isUnit()) {
			diff.addUnit(dt[pos + 1]);
			parsePos.setIndex(pos + 2);
			return true;
		}
		if (pos < dt.length && dt[pos].isUnit()) {
			diff.addUnit(dt[pos]);
			parsePos.setIndex(pos + 1);
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseOrdMonth --
	 * 
	 * Parse a relative month and sets the date difference. A relative month is
	 * valid, if it confirms to the following yacc rule: ordMonth: tNEXT tMONTH
	 * | tNEXT tUNUMBER tMONTH ;
	 * 
	 * Results: True, if a relative month was read (parsePos was incremented and
	 * the time difference was set according to the read relative time unit);
	 * false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseOrdMonth(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			ClockRelTimespan diff) // time difference to evaluate
	{
		int pos = parsePos.getIndex();

		if (pos + 2 < dt.length && dt[pos].is(ClockToken.NEXT) && dt[pos + 1].isUNumber()
				&& dt[pos + 2].is(ClockToken.MONTH)) {
			diff.addOrdMonth(dt[pos + 2].getInt(), dt[pos + 1].getInt());
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].is(ClockToken.NEXT) && dt[pos + 1].is(ClockToken.MONTH)) {
			diff.addOrdMonth(dt[pos + 1].getInt(), 1);
			parsePos.setIndex(pos + 2);
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseIso --
	 * 
	 * Parse an ISO 8601 point in time and sets the Calendar. An ISO 8601 point
	 * in time is valid, if it confirms to the following yacc rule: iso :
	 * tISOBASE tZONE tISOBASE | tISOBASE tZONE tUNUMBER ':' tUNUMBER ':'
	 * tUNUMBER | tISOBASE tISOBASE ;
	 * 
	 * Results: True, if an ISO 8601 point in time was read (parsePos was
	 * incremented and calendar was set according to the read point); false
	 * otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseIso(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			Calendar calendar) // calendar object to set
	{
		int pos = parsePos.getIndex();

		if (pos + 6 < dt.length && dt[pos].isIsoBase() && dt[pos + 1].is(ClockToken.ZONE) && dt[pos + 2].isUNumber()
				&& dt[pos + 3].is(':') && dt[pos + 4].isUNumber() && dt[pos + 5].is(':') && dt[pos + 6].isUNumber()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt() % 100);
			calendar.set(Calendar.MONTH, (dt[pos].getInt() % 10000) / 100 - 1);
			calendar.set(Calendar.YEAR, dt[pos].getInt() / 10000);
			calendar.set(Calendar.HOUR_OF_DAY, dt[pos + 2].getInt());
			calendar.set(Calendar.MINUTE, dt[pos + 4].getInt());
			calendar.set(Calendar.SECOND, dt[pos + 6].getInt());
			parsePos.setIndex(pos + 7);
			return true;
		}
		if (pos + 2 < dt.length && dt[pos].isIsoBase() && dt[pos + 1].is(ClockToken.ZONE)
				&& dt[pos + 1].getZone().getRawOffset() == -7 * MILLIS_PER_HOUR && dt[pos + 2].isIsoBase()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt() % 100);
			calendar.set(Calendar.MONTH, (dt[pos].getInt() % 10000) / 100 - 1);
			calendar.set(Calendar.YEAR, dt[pos].getInt() / 10000);
			calendar.set(Calendar.HOUR_OF_DAY, dt[pos + 2].getInt() / 10000);
			calendar.set(Calendar.MINUTE, (dt[pos + 2].getInt() % 10000) / 100);
			calendar.set(Calendar.SECOND, dt[pos + 2].getInt() % 100);
			parsePos.setIndex(pos + 3);
			return true;
		}
		if (pos + 1 < dt.length && dt[pos].isIsoBase() && dt[pos + 1].isIsoBase()) {
			calendar.set(Calendar.DAY_OF_MONTH, dt[pos].getInt() % 100);
			calendar.set(Calendar.MONTH, (dt[pos].getInt() % 10000) / 100 - 1);
			calendar.set(Calendar.YEAR, dt[pos].getInt() / 10000);
			calendar.set(Calendar.HOUR_OF_DAY, dt[pos + 1].getInt() / 10000);
			calendar.set(Calendar.MINUTE, (dt[pos + 1].getInt() % 10000) / 100);
			calendar.set(Calendar.SECOND, dt[pos + 1].getInt() % 100);
			parsePos.setIndex(pos + 2);
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseTrek --
	 * 
	 * Parse a Stardate and sets the Calendar. A Stardate is valid, if it
	 * confirms to the following yacc rule: iso : tSTARDATE tUNUMBER '.'
	 * tUNUMBER ;
	 * 
	 * Results: True, if a Stardate was read (parsePos was incremented and
	 * calendar was set according to the read point); false otherwise.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private boolean ParseTrek(ClockToken[] dt, // Input as scanned array of
			// tokens
			ParsePosition parsePos, // Current position in input
			GregorianCalendar calendar) // calendar object to set
	{
		int pos = parsePos.getIndex();

		if (pos + 3 < dt.length && dt[pos].is(ClockToken.STARDATE) && dt[pos + 1].isUNumber() && dt[pos + 2].is('.')
				&& dt[pos + 3].isUNumber()) {
			int trekYear = dt[pos + 1].getInt() / 1000 + 2323 - 377;
			int trekDay = 1 + ((dt[pos + 1].getInt() % 1000) * (calendar.isLeapYear(trekYear) ? 366 : 365)) / 1000;
			int trekSeconds = dt[pos + 3].getInt() * 144 * 60;
			calendar.set(Calendar.YEAR, trekYear);
			calendar.set(Calendar.DAY_OF_YEAR, trekDay);
			calendar.set(Calendar.SECOND, trekSeconds);
			parsePos.setIndex(pos + 4);
			return true;
		}
		return false;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * ParseMeridianAndSetHour --
	 * 
	 * Parse a meridian and sets the hour field of the calendar. A meridian is
	 * valid, if it confirms to the following yacc rule: o_merid : // NULL |
	 * tMERIDIAN ;
	 * 
	 * Results: None; parsePos was incremented and the claendar was set
	 * according to the read meridian.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private void ParseMeridianAndSetHour(ClockToken[] dt, // Input as scanned
			// array of tokens
			ParsePosition parsePos, // Current position in input
			Calendar calendar, // calendar object to set
			int hour) // hour value (1-12 or 0-23) to set.
	{
		int pos = parsePos.getIndex();
		int hourField;

		if (pos < dt.length && dt[pos].is(ClockToken.MERIDIAN)) {
			calendar.set(Calendar.AM_PM, dt[pos].getInt());
			parsePos.setIndex(pos + 1);
			hourField = Calendar.HOUR;
		} else {
			hourField = Calendar.HOUR_OF_DAY;
		}

		if (hourField == Calendar.HOUR && hour == 12) {
			hour = 0;
		}
		calendar.set(hourField, hour);
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * GetTokens --
	 * 
	 * Lexical analysis of the input string.
	 * 
	 * Results: An array of ClockToken, representing the input string.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private ClockToken[] GetTokens(String in, // String to parse
			boolean debug) // Send the generated token list to stderr?
	{
		ParsePosition parsePos = new ParsePosition(0);
		ClockToken dt;
		ArrayList tokens = new ArrayList(in.length());

		while ((dt = GetNextToken(in, parsePos)) != null) {
			tokens.add(dt);
		}

		ClockToken[] tokenArray = { (ClockToken) null };
		tokenArray = (ClockToken[]) tokens.toArray(tokenArray);

		if (debug) {
			for (int ix = 0; ix < tokenArray.length; ix++) {
				if (ix != 0) {
					System.err.print(",");
				}
				System.err.print(tokenArray[ix].toString());
			}
			System.err.println("");
		}

		return tokenArray;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * GetNextToken --
	 * 
	 * Lexical analysis of the next token of input string.
	 * 
	 * Results: A ClockToken representing the next token of the input string,
	 * (parsePos was incremented accordingly), if one was found. null otherwise
	 * (e.g. at end of input).
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private ClockToken GetNextToken(String in, // String to parse
			ParsePosition parsePos) // Current position in input
	{
		int pos = parsePos.getIndex();
		int sign;

		while (true) {
			while (pos < in.length() && Character.isSpaceChar(in.charAt(pos))) {
				pos++;
			}
			if (pos >= in.length()) {
				break;
			}

			char c = in.charAt(pos);
			if (Character.isDigit(c)) {
				int number = 0;
				int count = 0;
				while (pos < in.length() && Character.isDigit(c = in.charAt(pos))) {
					number = 10 * number + c - '0';
					pos++;
					count++;
				}
				parsePos.setIndex(pos);
				return new ClockToken(number, count >= 6);
			}
			if (Character.isLetter(c)) {
				int beginPos = pos;
				while (++pos < in.length()) {
					c = in.charAt(pos);
					if (!Character.isLetter(c) && c != '.') {
						break;
					}
				}
				parsePos.setIndex(pos);
				return LookupWord(in.substring(beginPos, pos));
			}
			parsePos.setIndex(pos + 1);
			return new ClockToken(in.charAt(pos));
		}
		parsePos.setIndex(pos + 1);
		return null;
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * LookupWord --
	 * 
	 * Construct a ClockToken for the given word.
	 * 
	 * Results: A ClockToken representing the given word.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private ClockToken LookupWord(String word) // word to lookup
	{
		int ix;
		String[] names;
		String[][] zones;

		if (word.equalsIgnoreCase("am") || word.equalsIgnoreCase("a.m.")) {
			return new ClockToken(ClockToken.MERIDIAN, Calendar.AM);
		}
		if (word.equalsIgnoreCase("pm") || word.equalsIgnoreCase("p.m.")) {
			return new ClockToken(ClockToken.MERIDIAN, Calendar.PM);
		}

		// See if we have an abbreviation for a day or month.

		boolean abbrev;
		if (word.length() == 3) {
			abbrev = true;
		} else if (word.length() == 4 && word.charAt(3) == '.') {
			abbrev = true;
			word = word.substring(0, 3);
		} else {
			abbrev = false;
		}

		DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
		if (abbrev) {
			names = symbols.getShortMonths();
		} else {
			names = symbols.getMonths();
		}
		for (ix = 0; ix < names.length; ix++) {
			if (word.equalsIgnoreCase(names[ix])) {
				return new ClockToken(ClockToken.MONTH, ix);
			}
		}
		if (abbrev) {
			names = symbols.getShortWeekdays();
		} else {
			names = symbols.getWeekdays();
		}
		for (ix = 0; ix < names.length; ix++) {
			if (word.equalsIgnoreCase(names[ix])) {
				return new ClockToken(ClockToken.DAY, ix);
			}
		}

		// Drop out any periods and try the timezone table.

		StringBuffer withoutDotsBuf = new StringBuffer(word.length());
		for (ix = 0; ix < word.length(); ix++) {
			if (word.charAt(ix) != '.') {
				withoutDotsBuf.append(word.charAt(ix));
			}
		}

		String withoutDots = new String(withoutDotsBuf);
		zones = symbols.getZoneStrings();

		for (ix = 0; ix < zones.length; ix++) {
			if (withoutDots.equalsIgnoreCase(zones[ix][2]) || withoutDots.equalsIgnoreCase(zones[ix][4])) {
				TimeZone zone = TimeZone.getTimeZone(zones[ix][0]);
				return new ClockToken(ClockToken.ZONE, zone);
			}
		}
		if (withoutDots.equalsIgnoreCase("dst")) {
			return new ClockToken(ClockToken.DST, null);
		}

		// Strip off any plural and try the units.

		String singular;
		if (word.endsWith("s")) {
			singular = word.substring(0, word.length() - 1);
		} else {
			singular = word;
		}
		if (singular.equalsIgnoreCase("year")) {
			return new ClockToken(ClockToken.MONTH_UNIT, 12);
		} else if (singular.equalsIgnoreCase("month")) {
			return new ClockToken(ClockToken.MONTH_UNIT, 1);
		} else if (singular.equalsIgnoreCase("fortnight")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 14 * 24 * 60);
		} else if (singular.equalsIgnoreCase("week")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 7 * 24 * 60);
		} else if (singular.equalsIgnoreCase("day")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 24 * 60);
		} else if (singular.equalsIgnoreCase("hour")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 60);
		} else if (singular.equalsIgnoreCase("minute")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 1);
		} else if (singular.equalsIgnoreCase("min")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 1);
		} else if (singular.equalsIgnoreCase("second")) {
			return new ClockToken(ClockToken.SEC_UNIT, 1);
		} else if (singular.equalsIgnoreCase("sec")) {
			return new ClockToken(ClockToken.SEC_UNIT, 1);
		}

		if (singular.equalsIgnoreCase("tomorrow")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 1 * 24 * 60);
		} else if (singular.equalsIgnoreCase("yesterday")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, -1 * 24 * 60);
		} else if (singular.equalsIgnoreCase("today")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 0);
		} else if (singular.equalsIgnoreCase("now")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 0);
		} else if (singular.equalsIgnoreCase("last")) {
			return new ClockToken(-1, false);
		} else if (singular.equalsIgnoreCase("this")) {
			return new ClockToken(ClockToken.MINUTE_UNIT, 0);
		} else if (singular.equalsIgnoreCase("next")) {
			return new ClockToken(ClockToken.NEXT, 1);
		} else if (singular.equalsIgnoreCase("ago")) {
			return new ClockToken(ClockToken.AGO, 1);
		} else if (singular.equalsIgnoreCase("epoch")) {
			return new ClockToken(ClockToken.EPOCH, 0);
		} else if (singular.equalsIgnoreCase("stardate")) {
			return new ClockToken(ClockToken.STARDATE, 0);
		}

		// Since a military timezone (T) is used in the clock test of 8.3,
		// we can't ignore these timezones any longer...

		if (withoutDots.length() == 1) {
			int rawOffset = 0;
			boolean found = true;
			char milTz = Character.toLowerCase(withoutDots.charAt(0));

			if (milTz >= 'a' && milTz <= 'm') {
				rawOffset = milTz - 'a' + 1;
			} else if (milTz >= 'n' && milTz < 'z') {
				rawOffset = 'n' - milTz - 1;
			} else if (milTz != 'z') {
				found = false;
			}
			if (found) {
				ClockToken zone = GetTimeZoneFromRawOffset(rawOffset);
				if (zone != null) {
					return zone;
				}
			}
		}

		return new ClockToken(word);
	}

	/**
	 *------------------------------------------------------------------------
	 * -----
	 * 
	 * GetTimeZoneFromRawOffset --
	 * 
	 * Look for a timezone with the given offset (in hours) from gmt.
	 * 
	 * Results: A ClockToken representing the specified timezone.
	 * 
	 * Side effects: None.
	 * 
	 *------------------------------------------------------------------------
	 * -----
	 */

	private ClockToken GetTimeZoneFromRawOffset(int rawOffset // an offset to
	// GMT (in
	// hours).
	) {
		String tzNames[] = TimeZone.getAvailableIDs(rawOffset * MILLIS_PER_HOUR);

		if (tzNames.length > 0) {
			TimeZone zone = TimeZone.getTimeZone(tzNames[0]);
			return new ClockToken(ClockToken.ZONE, zone);
		}
		return null;
	}

} // end ClockCmd

/**
 *-----------------------------------------------------------------------------
 * 
 * CLASS ClockToken --
 * 
 * An object of this class represents a lexical unit of the human readable date
 * string. It can be one of the following variants:
 * 
 * - unsigned number, = occurence can be asked by isUNumber(), = value can be
 * retrieved by means of getInt(); - iso base date, = occurence can be asked by
 * isIsoBase(), = value can be retrieved by means of getInt(); - a single
 * character (delimiters like ':' or '/'), = occurence can be asked by is(),
 * e.g. is('/'); - a word (like "January" or "DST") = occurence can be asked by
 * is(), e.g. is(ClockToken.AGO); = value can be retrieved by means of getInt()
 * or getZone().
 * 
 *-----------------------------------------------------------------------------
 */

class ClockToken {
	final static int ISOBASE = 1;
	final static int UNUMBER = 2;
	final static int WORD = 3;
	final static int CHAR = 4;
	final static int MONTH = 5;
	final static int DAY = 6;
	final static int MONTH_UNIT = 7;
	final static int MINUTE_UNIT = 8;
	final static int SEC_UNIT = 9;
	final static int AGO = 10;
	final static int EPOCH = 11;
	final static int ZONE = 12;
	final static int DAYZONE = 13;
	final static int DST = 14;
	final static int MERIDIAN = 15;
	final static int NEXT = 16;
	final static int STARDATE = 17;

	ClockToken(int number, boolean isIsoBase) {
		this.kind = isIsoBase ? ISOBASE : UNUMBER;
		this.number = number;
	}

	ClockToken(int kind, int number) {
		this.kind = kind;
		this.number = number;
	}

	ClockToken(int kind, TimeZone zone) {
		this.kind = kind;
		this.zone = zone;
	}

	ClockToken(String word) {
		this.kind = WORD;
		this.word = word;
	}

	ClockToken(char c) {
		this.kind = CHAR;
		this.c = c;
	}

	public boolean isUNumber() {
		return kind == UNUMBER;
	}

	public boolean isIsoBase() {
		return kind == ISOBASE;
	}

	public boolean is(char c) {
		return this.kind == CHAR && this.c == c;
	}

	public boolean is(int kind) {
		return this.kind == kind;
	}

	public boolean isUnit() {
		return kind == MINUTE_UNIT || kind == MONTH_UNIT || kind == SEC_UNIT;
	}

	int getInt() {
		return number;
	}

	TimeZone getZone() {
		return zone;
	}

	public String toString() {
		if (isUNumber()) {
			return "U" + Integer.toString(getInt());
		} else if (isIsoBase()) {
			return "I" + Integer.toString(getInt());
		} else if (kind == WORD) {
			return word;
		} else if (kind == CHAR) {
			return new Character(c).toString();
		} else if (kind == ZONE || kind == DAYZONE) {
			return zone.getID();
		} else {
			return "(" + kind + "," + getInt() + ")";
		}
	}

	private int kind;
	private int number;
	private String word;
	private char c;
	private TimeZone zone;
} // end ClockToken

/**
 *-----------------------------------------------------------------------------
 * 
 * CLASS ClockRelTimespan --
 * 
 * An object of this class can be used to track the time difference during the
 * analysis of a relative time specification.
 * 
 * It has four read only properties seconds, months, weekday and dayOrdinal,
 * which are set to 0 during initialization and which can be modified by means
 * of the addSeconds(), addMonths(), setWeekday() and negate() methods.
 * 
 *-----------------------------------------------------------------------------
 */

class ClockRelTimespan {
	ClockRelTimespan() {
		seconds = 0;
		months = 0;
		ordMonth = 0;
		weekday = 0;
		dayOrdinal = 0;
	}

	void addSeconds(int s) {
		seconds += s;
	}

	void addMonths(int m) {
		months += m;
	}

	void addOrdMonth(int m, int c) {
		months = m;
		ordMonth += c;
	}

	void addUnit(ClockToken unit, int amount) {
		if (unit.is(ClockToken.SEC_UNIT)) {
			addSeconds(unit.getInt() * amount);
		} else if (unit.is(ClockToken.MINUTE_UNIT)) {
			addSeconds(unit.getInt() * 60 * amount);
		} else if (unit.is(ClockToken.MONTH_UNIT)) {
			addMonths(unit.getInt() * amount);
		}
	}

	void addUnit(ClockToken unit) {
		addUnit(unit, 1);
	}

	void setWeekday(int w, int ord) {
		weekday = w;
		dayOrdinal = ord;
	}

	void setWeekday(int w) {
		setWeekday(w, 1);
	}

	void negate() {
		seconds = -seconds;
		months = -months;
	}

	int getSeconds() {
		return seconds;
	}

	int getMonths() {
		return months;
	}

	int getOrdMonth() {
		return ordMonth;
	}

	int getWeekday() {
		return weekday;
	}

	int getDayOrdinal() {
		return dayOrdinal;
	}

	private int seconds;
	private int months;
	private int ordMonth;
	private int weekday;
	private int dayOrdinal;
}
