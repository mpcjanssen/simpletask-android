/**
 * @copyright 2014- Mark Janssen)
 */
package nl.mpcjanssen.simpletask;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;
import de.greenrobot.dao.query.QueryBuilder;
import nl.mpcjanssen.simpletask.dao.gen.TodoFile;
import nl.mpcjanssen.simpletask.util.Util;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HistoryScreen extends ThemedActivity {

    private Logger log;
    private Cursor m_cursor;
    private Menu toolbar_menu;
    private int mScroll = 0;
    private TodoApplication m_app;
    private static String TAG = "HistoryScreen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = Logger.INSTANCE;
        m_app = (TodoApplication) getApplication();

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
        QueryBuilder<TodoFile> builder = m_app.daoSession.getTodoFileDao().queryBuilder();
        m_cursor = builder.buildCursor().query();
        m_cursor.moveToLast();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (m_cursor !=null) {
            m_cursor.close();
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
            log.warn(TAG, "Failed to create file for sharing", e);
        }
        startActivity(Intent.createChooser(shareIntent, "Share History Database"));

    }

    @NotNull
    private File getDatabaseFile() {
        return new File(m_app.daoSession.getDatabase().getPath());
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
                        if (m_cursor.getCount()==0) {
                            Util.showToastShort(HistoryScreen.this, "Nothing to share");
                        } else {
                            Util.shareText(HistoryScreen.this, "Old todo version", getCurrentFileContents());
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
        log.info(TAG, "Clearing history database");
        m_app.backupDao.deleteAll();
        initCursor();
        displayCurrent();
    }

    private void showNext() {
        saveScroll();
        m_cursor.moveToNext();
        displayCurrent();
    }

    private void saveScroll() {
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
        mScroll = sv.getScrollY();
    }


    private void showPrev() {
        saveScroll();
        m_cursor.moveToPrevious();
        displayCurrent();
    }

    private void displayCurrent() {
        String todoContents = "no history";
        long date = 0L;
        String name = "";
        if (this.m_cursor.getCount() != 0) {
            todoContents = getCurrentFileContents();
            date = this.m_cursor.getLong(2);
            name = this.m_cursor.getString(1);

        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        TextView fileView = (TextView) findViewById(R.id.history_view);

        TextView nameView = (TextView) findViewById(R.id.name);
        TextView dateView = (TextView) findViewById(R.id.date);
        ScrollView sv = (ScrollView) findViewById(R.id.scrollView);
        fileView.setText(todoContents);
        nameView.setText(name);
        dateView.setText(format.format(new Date(date)));
        sv.setScrollY(mScroll);
        updateMenu();
    }

    private String getCurrentFileContents() {
        return m_cursor.getString(0);
    }

    private void updateMenu() {
        if (toolbar_menu == null || m_cursor == null) {
            return;
        }
        MenuItem prev = toolbar_menu.findItem(R.id.menu_prev);
        MenuItem next = toolbar_menu.findItem(R.id.menu_next);
        if (m_cursor.isFirst() || m_cursor.getCount() < 2) {
            prev.setEnabled(false);
        } else {
            prev.setEnabled(true);
        }
        if (m_cursor.isLast() ||  m_cursor.getCount() < 2) {
            next.setEnabled(false);
        } else {
            next.setEnabled(true);
        }
    }
}
