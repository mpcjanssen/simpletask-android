package nl.mpcjanssen.simpletask;

import android.app.Activity;
import android.os.Bundle;

class ThemedActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TodoApplication app = (TodoApplication) getApplication();
        setTheme(app.getActiveTheme());
        super.onCreate(savedInstanceState);
    }
}
