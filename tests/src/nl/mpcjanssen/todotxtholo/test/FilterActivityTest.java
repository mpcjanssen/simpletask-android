package nl.mpcjanssen.todotxtholo.test;

import android.test.ActivityInstrumentationTestCase2;
import nl.mpcjanssen.todotxtholo.FilterActivity;

public class FilterActivityTest extends ActivityInstrumentationTestCase2<FilterActivity> {

    public FilterActivityTest() {

        super(FilterActivity.class);
    }


    public void testStart() throws Exception {
        assertEquals(true, true);
    }
}
