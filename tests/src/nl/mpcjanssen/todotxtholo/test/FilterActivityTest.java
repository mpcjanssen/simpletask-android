package nl.mpcjanssen.simpletask.test;

import android.test.ActivityInstrumentationTestCase2;
import nl.mpcjanssen.simpletask.FilterActivity;

public class FilterActivityTest extends ActivityInstrumentationTestCase2<FilterActivity> {

    public FilterActivityTest() {

        super(FilterActivity.class);
    }


    public void testStart() throws Exception {
        assertEquals(true, true);
    }
}
