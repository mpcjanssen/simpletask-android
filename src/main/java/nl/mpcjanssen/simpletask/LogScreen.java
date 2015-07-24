/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import com.google.common.io.LineProcessor;
import nl.mpcjanssen.simpletask.adapters.LogAdapter;
import nl.mpcjanssen.simpletask.util.TaskIo;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.danoz.recyclerviewfastscroller.vertical.VerticalRecyclerViewFastScroller;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class LogScreen extends ThemedActivity {

    private Logger log;
    private SQLiteDatabase db;
    private Cursor cursor;
    private Menu toolbar_menu;
    private int mScroll = 0;
    private TodoApplication m_app;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ArrayList<String> myDataset;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = LoggerFactory.getLogger(this.getClass());
        m_app = (TodoApplication) getApplication();
        setContentView(R.layout.log);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(false);

        VerticalRecyclerViewFastScroller fastScroller = (VerticalRecyclerViewFastScroller) findViewById(R.id.fast_scroller);

        // Connect the recycler to the scroller (to let the scroller scroll the list)
        fastScroller.setRecyclerView(mRecyclerView);

        // Connect the scroller to the recycler (to let the recycler scroll the scroller's handle)
        mRecyclerView.addOnScrollListener(fastScroller.getOnScrollListener());

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        myDataset = new ArrayList<>();
        File logFile = getLogFile();
        try {
            TaskIo.loadFromFile(logFile, new LineProcessor<String>() {
                @Override
                public boolean processLine(String line) throws IOException {
                    if (!line.trim().isEmpty()){
                        myDataset.add(line);
                    }
                    return true;
                }

                @Override
                public String getResult() {
                    return null;
                }
            });
        } catch (IOException e) {
            log.error("Failed to load logfile", e);
        }

        // specify an adapter (see also next example)
        LogAdapter mAdapter = new LogAdapter(myDataset);
        mRecyclerView.setAdapter(mAdapter);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor!=null) {
            cursor.close();
        }
        if (db!=null) {
            db.close();
        }
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
            log.warn("Failed to create file for sharing");
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
        toolbar_menu = toolbar.getMenu();
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
