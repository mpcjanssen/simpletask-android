/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ScrollView;
import android.widget.TextView;
import com.github.rjeschke.txtmark.Processor;
import com.melnykov.fab.FloatingActionButton;
import hirondelle.date4j.DateTime;
import nl.mpcjanssen.simpletask.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Stack;
import java.util.TimeZone;

public class HistoryScreen extends ThemedActivity {

    private Logger log;
    private SQLiteDatabase db;
    private Cursor cursor;
    private Menu toolbar_menu;
    private int mScroll = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = LoggerFactory.getLogger(this.getClass());
        TodoApplication m_app = (TodoApplication) getApplication();
        BackupDbHelper backupDbHelper = new BackupDbHelper(this.getApplication().getApplicationContext());
        // Gets the data repository in read mode
        db = backupDbHelper.getReadableDatabase();
        cursor = db.query(BackupDbHelper.TABLE_NAME, null, null, null, null, null, BackupDbHelper.FILE_DATE, null);
        cursor.moveToLast();
        setContentView(R.layout.history);
        displayCurrent();
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
        File dataDir = new File(this.getApplicationInfo().dataDir);
        File databaseDir = new File(dataDir, "databases");
        File dataBase = new File(databaseDir, BackupDbHelper.DATABASE_NAME);
        try {
            Util.createCachedDatabase(this, dataBase);
            Uri fileUri = Uri.parse("content://" + CachedFileProvider.AUTHORITY + "/" + "history.db");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        } catch (Exception e) {
            log.warn("Failed to create file for sharing");
        }
        startActivity(Intent.createChooser(shareIntent, "Share History Database"));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);MenuInflater inflater = getMenuInflater();
        toolbar_menu = toolbar.getMenu();
        toolbar_menu.clear();
        inflater.inflate(R.menu.history_menu, toolbar_menu);
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
                    case R.id.menu_share:
                        Util.shareText(HistoryScreen.this, getCurrentFileContents());
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

    private void showNext() {
        saveScroll();
        cursor.moveToNext();
        displayCurrent();
        updateMenu();
    }

    private void saveScroll() {
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
        mScroll = sv.getScrollY();
    }


    private void showPrev() {
        saveScroll();
        cursor.moveToPrevious();
        displayCurrent();
        updateMenu();
    }

    private void displayCurrent() {
        String todoContents = getCurrentFileContents();
        String date = cursor.getString(cursor.getColumnIndex(BackupDbHelper.FILE_DATE));
        String name = cursor.getString(cursor.getColumnIndex(BackupDbHelper.FILE_NAME));
        TextView fileView = (TextView) findViewById(R.id.history_view);

        TextView nameView = (TextView) findViewById(R.id.name);
        TextView dateView = (TextView) findViewById(R.id.date);
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
        fileView.setText(todoContents);
        nameView.setText(name);
        dateView.setText(date);
        sv.setScrollY(mScroll);
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
        if (cursor.isFirst()) {
            prev.setEnabled(false);
        } else {
            prev.setEnabled(true);
        }
        if (cursor.isLast()) {
            next.setEnabled(false);
        } else {
            next.setEnabled(true);
        }
    }
}
