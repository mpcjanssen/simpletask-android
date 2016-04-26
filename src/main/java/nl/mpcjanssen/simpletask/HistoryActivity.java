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

public class HistoryActivity extends ThemedActivity {

    private Logger log;
    private Cursor cursor;
    private Menu toolbar_menu;
    private int mScroll = 0;
    private SimpletaskApplication m_app;
    private static String TAG = "HistoryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log = Logger.INSTANCE;
        m_app = (SimpletaskApplication) getApplication();

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
        cursor = builder.buildCursor().query();
        cursor.moveToLast();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cursor!=null) {
            cursor.close();
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
                        if (cursor.getCount()==0) {
                            Util.showToastShort(HistoryActivity.this, "Nothing to share");
                        } else {
                            Util.shareText(HistoryActivity.this, "Old todo version", getCurrentFileContents());
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
        long date = 0L;
        String name = "";
        if (cursor.getCount() != 0) {
            todoContents = getCurrentFileContents();
            date = cursor.getLong(2);
            name = cursor.getString(1);

        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
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
        return cursor.getString(0);
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
