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

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import nl.mpcjanssen.simpletask.dao.gen.LogItem;
import nl.mpcjanssen.simpletask.dao.gen.TodoModificationItem;
import nl.mpcjanssen.simpletask.dao.gen.TodoModificationItemDao;
import nl.mpcjanssen.simpletask.util.Util;

public class ModificationScreen extends ThemedActivity {

    private TodoApplication m_app;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log);
        ListView mListView = (ListView) findViewById(R.id.listView);

        mListView.setFastScrollEnabled(true);

        ArrayList<String> myDataset = new ArrayList<>();
        m_app = (TodoApplication) getApplication();
        for (TodoModificationItem entry : m_app.todoModificationDao.queryBuilder().orderDesc(TodoModificationItemDao.Properties.Id).list()) {
            String line = logItemToString(entry);
            myDataset.add(line);
        }

        // specify an adapter (see also next example)
        ArrayAdapter<String> mAdapter = new ArrayAdapter<>(this, R.layout.log_item, myDataset);
        mListView.setAdapter(mAdapter);

    }

    @NotNull
    private String logItemToString(TodoModificationItem entry) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", Locale.US);
        return format.format(entry.getTimestamp()) + "\t"
                        + entry.getFile() + "\t"
                        + entry.getAction() + "\n"
                        + entry.getFrom() + "\n"
                        + entry.getTo();
    }

    private void sendLog() {
        StringBuilder logContents = new StringBuilder();
        for (TodoModificationItem item : m_app.todoModificationDao.loadAll()){
            logContents.append(logItemToString(item)).append("\n");
        }
        Util.shareText(ModificationScreen.this, logContents.toString() );
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
