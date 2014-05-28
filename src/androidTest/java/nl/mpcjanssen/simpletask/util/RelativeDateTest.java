package nl.mpcjanssen.simpletask.util;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Calendar;


/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */

public class RelativeDateTest extends TestCase {
    public void testMonthWrap()  {
        // Bug f35cd1b
        DateTimeFormatter df = ISODateTimeFormat.date();
        DateTime now = df.parseDateTime("2013-10-01");
        DateTime when = df.parseDateTime("2013-09-30");
        assertEquals("1 day ago", RelativeDate.computeRelativeDate(now,when));
    }
}
