/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LogScreen extends ThemedActivity {

    private String TAG ="LogScreen";
    private Logger log;

    private ArrayList<String> myDataset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = Logger.INSTANCE;
        setContentView(R.layout.log);
        ListView mListView = (ListView) findViewById(R.id.listView);

        mListView.setFastScrollEnabled(true);

        myDataset = new ArrayList<>();
        File logFile = getLogFile();
        try {
            for (String line : TaskIo.loadFromFile(logFile)) {

                if (!line.trim().isEmpty()) {
                    myDataset.add(line);
                }
            }


        } catch (IOException e) {
            log.error(TAG, "Failed to load logfile", e);
        }

        // specify an adapter (see also next example)
        ArrayAdapter mAdapter = new ArrayAdapter(this, R.layout.log_item, myDataset);
        mListView.setAdapter(mAdapter);

    }

    private void sendLog() {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Simpletask Logging File");
        File dataBase = getLogFile();
        try {
            Util.createCachedDatabase(this, dataBase);
            Uri fileUri = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/" + "log.txt");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        } catch (Exception e) {
            log.warn(TAG, "Failed to create file for sharing");
        }
        startActivity(Intent.createChooser(shareIntent, "Share Logging File"));

    }

    @NotNull
    private File getLogFile() {
        File dataDir = new File(getApplicationInfo().dataDir);
        File databaseDir = new File(dataDir, "files");
        return new File(databaseDir, "log.txt");
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
