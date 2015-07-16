package nl.mpcjanssen.simpletask;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;

/**
 * Created by Mark on 2015-07-16.
 */
public class ThemedFragmentActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TodoApplication app = (TodoApplication) getApplication();
        setTheme(app.getActiveTheme());
        setTheme(app.getActiveFont());
        super.onCreate(savedInstanceState);
    }
}
