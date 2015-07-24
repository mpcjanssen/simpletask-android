/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class HistoryScreen extends ThemedActivity {

    private Logger log;
    private SQLiteDatabase db;
    private Cursor cursor;
    private Menu toolbar_menu;
    private int mScroll = 0;
    private TodoApplication m_app;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = LoggerFactory.getLogger(this.getClass());
        m_app = (TodoApplication) getApplication();
        BackupDbHelper backupDbHelper = new BackupDbHelper(this.getApplication().getApplicationContext());
        db = backupDbHelper.getReadableDatabase();
        // Gets the data repository in read mode
        initCursor();
        File dbFile = getDatabaseFile();
        if (dbFile.exists()) {
            String title = getTitle().toString();
            title = title + " (" + dbFile.length()/1024 + "KB)";
            setTitle(title);
        }
        setContentView(R.layout.history);
        displayCurrent();
    }

    private void initCursor() {
        cursor = db.query(BackupDbHelper.TABLE_NAME, null, null, null, null, null, BackupDbHelper.FILE_DATE, null);
        cursor.moveToLast();
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

    private void shareHistory() {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("application/x-sqlite3");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "Simpletask History Database");
        File dataBase = getDatabaseFile();
        try {
            Util.createCachedDatabase(this, dataBase);
            Uri fileUri = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/" + dataBase.getName());
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        } catch (Exception e) {
            log.warn("Failed to create file for sharing", e);
        }
        startActivity(Intent.createChooser(shareIntent, "Share History Database"));

    }

    @NotNull
    private File getDatabaseFile() {
        File dataDir = new File(m_app.getApplicationInfo().dataDir);
        File databaseDir = new File(dataDir, "databases");
        return new File(databaseDir, BackupDbHelper.DATABASE_NAME);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);MenuInflater inflater = getMenuInflater();
        toolbar_menu = toolbar.getMenu();
        toolbar_menu.clear();
        inflater.inflate(R.menu.history_menu, toolbar_menu);
        updateMenu();
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    // Respond to the action bar's Up/Home button
                    case R.id.menu_prev:
                        showPrev();
                        return true;
                    case R.id.menu_next:
                        showNext();
                        return true;
                    case R.id.menu_clear_database:
                        clearDatabase();
                        return true;
                    case R.id.menu_share:
                        if (cursor.getCount()==0) {
                            Util.showToastShort(HistoryScreen.this, "Nothing to share");
                        } else {
                            Util.shareText(HistoryScreen.this, getCurrentFileContents());
                        }
                        return true;
                    case R.id.menu_share_database:
                        shareHistory();
                        return true;
                }
                return true;
            }
        });
        return true;
    }

    private void clearDatabase() {
        log.info("Clearing history database");
        BackupDbHelper backupDbHelper = new BackupDbHelper(this.getApplication().getApplicationContext());
        SQLiteDatabase database = backupDbHelper.getWritableDatabase();
        database.delete(BackupDbHelper.TABLE_NAME, null, null);
        database.close();
        initCursor();
        displayCurrent();
    }

    private void showNext() {
        saveScroll();
        cursor.moveToNext();
        displayCurrent();
    }

    private void saveScroll() {
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
        mScroll = sv.getScrollY();
    }


    private void showPrev() {
        saveScroll();
        cursor.moveToPrevious();
        displayCurrent();
    }

    private void displayCurrent() {
        String todoContents = "no history";
        String date = "";
        String name = "";
        if (cursor.getCount() != 0) {
            todoContents = getCurrentFileContents();
            date = cursor.getString(cursor.getColumnIndex(BackupDbHelper.FILE_DATE));
            name = cursor.getString(cursor.getColumnIndex(BackupDbHelper.FILE_NAME));

        }

        TextView fileView = (TextView) findViewById(R.id.history_view);

        TextView nameView = (TextView) findViewById(R.id.name);
        TextView dateView = (TextView) findViewById(R.id.date);
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
        fileView.setText(todoContents);
        nameView.setText(name);
        dateView.setText(date);
        sv.setScrollY(mScroll);
        updateMenu();
    }

    private String getCurrentFileContents() {
        return cursor.getString(cursor.getColumnIndex(BackupDbHelper.FILE_ID));
    }

    private void updateMenu() {
        if (cursor == null || toolbar_menu == null) {
            return;
        }
        MenuItem prev = toolbar_menu.findItem(R.id.menu_prev);
        MenuItem next = toolbar_menu.findItem(R.id.menu_next);
        if (cursor.isFirst() || cursor.getCount()==0) {
            prev.setEnabled(false);
        } else {
            prev.setEnabled(true);
        }
        if (cursor.isLast() ||  cursor.getCount()==0) {
            next.setEnabled(false);
        } else {
            next.setEnabled(true);
        }
    }
}
