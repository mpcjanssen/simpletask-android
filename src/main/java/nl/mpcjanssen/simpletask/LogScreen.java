/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import nl.mpcjanssen.simpletask.dao.gen.LogItem;
import nl.mpcjanssen.simpletask.dao.gen.LogItemDao;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class LogScreen extends ThemedActivity {

    private TodoApplication m_app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);
        ListView mListView = (ListView) findViewById(R.id.listView);

        mListView.setFastScrollEnabled(true);

        ArrayList<String> myDataset = new ArrayList<>();
        m_app = (TodoApplication) getApplication();
        for (LogItem entry : m_app.logDao.queryBuilder().orderDesc(LogItemDao.Properties.Timestamp).list()) {
            String line = logItemToString(entry);
            myDataset.add(line);
        }

        // specify an adapter (see also next example)
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, R.layout.log_item, myDataset);
        mListView.setAdapter(mAdapter);

    }

    @NotNull
    private String logItemToString(LogItem entry) {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.S", Locale.US);
        return format.format(entry.getTimestamp()) + "\t"
                        + entry.getSeverity() + "\t"
                        + entry.getTag() + "\t"
                        + entry.getMessage() + "\t"
                        + entry.getException();
    }

    private void sendLog() {
        StringBuilder logContents = new StringBuilder();
        for (LogItem item : m_app.logDao.loadAll()){
            logContents.append(logItemToString(item)).append("\n");
        }
        Util.shareText(LogScreen.this, logContents.toString() );
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);MenuInflater inflater = getMenuInflater();
        Menu toolbar_menu = toolbar.getMenu();
        toolbar_menu.clear();
        inflater.inflate(R.menu.log_menu, toolbar_menu);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    // Respond to the action bar's Up/Home button

                    case R.id.menu_share:
                        sendLog();
                        return true;
                }
                return true;
            }
        });
        return true;
    }
}
