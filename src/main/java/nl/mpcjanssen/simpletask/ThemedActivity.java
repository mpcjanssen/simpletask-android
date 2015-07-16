package nl.mpcjanssen.simpletask;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

abstract class ThemedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TodoApplication app = (TodoApplication) getApplication();
        setTheme(app.getActiveTheme());
        setTheme(app.getActiveFont());
        super.onCreate(savedInstanceState);
    }
}
