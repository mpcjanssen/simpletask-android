package nl.mpcjanssen.simpletask.util;

import junit.framework.TestCase;


import java.util.Calendar;


/**
 * Created with IntelliJ IDEA.
 * User: Mark Janssen
 * Date: 21-7-13
 * Time: 12:28
 */
import hirondelle.date4j.DateTime;

public class RelativeDateTest extends TestCase {
    public void testMonthWrap()  {
        // Bug f35cd1b
        DateTime now = new DateTime("2013-10-01");
        DateTime when = new DateTime("2013-09-30");
        assertEquals("1 day ago", RelativeDate.computeRelativeDate(now,when));
    }
}
