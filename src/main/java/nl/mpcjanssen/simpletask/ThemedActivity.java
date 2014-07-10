package nl.mpcjanssen.simpletask;

import android.app.Activity;
import android.os.Bundle;

abstract class ThemedActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TodoApplication app = (TodoApplication) getApplication();
        setTheme(app.getActiveTheme());
        setTheme(app.getActiveFont());
        super.onCreate(savedInstanceState);
    }
}
