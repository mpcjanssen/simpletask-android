package nl.mpcjanssen.simpletask.util;

import junit.framework.TestCase;

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
        Calendar now = Calendar.getInstance();
        now.set(2013,Calendar.OCTOBER,1);
        Calendar cal = Calendar.getInstance();
        cal.set(2013,Calendar.SEPTEMBER,30);
        assertEquals("1 day ago", RelativeDate.computeRelativeDate(now,cal));
    }
}
