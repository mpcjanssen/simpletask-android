package nl.mpcjanssen.simpletask;

import android.app.ListActivity;
import android.os.Bundle;

abstract class ThemedListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TodoApplication app = (TodoApplication) getApplication();
        setTheme(app.getActiveTheme());
        super.onCreate(savedInstanceState);
    }
}
