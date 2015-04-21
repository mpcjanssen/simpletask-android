package nl.mpcjanssen.simpletask;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Window;

abstract class ThemedListActivity extends ListActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        TodoApplication app = (TodoApplication) getApplication();
        setTheme(app.getActiveTheme());
        setTheme(app.getActiveFont());
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
    }
}
