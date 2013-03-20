package nl.mpcjanssen.todotxtholo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TaskStorageAdapter {


    public static final String KEY_ROWID = "_id";
    public static final String KEY_LINE = "line";
    public static final String KEY_TEXT = "text";

    private static final String TAG = "TaskStorageAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;

    private static final String DATABASE_NAME = "Tasks";
    private static final String SQLITE_TABLE = "todo";
    private static final int DATABASE_VERSION = 1;

    private final Context mCtx;

    private static final String DATABASE_CREATE =
            "CREATE TABLE if not exists " + SQLITE_TABLE + " (" +
                    KEY_ROWID + " integer PRIMARY KEY autoincrement," +
                    KEY_LINE + "," +
                    KEY_TEXT + "," +
                    " UNIQUE (" + KEY_TEXT + "));";

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }


        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.w(TAG, DATABASE_CREATE);
            db.execSQL(DATABASE_CREATE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SQLITE_TABLE);
            onCreate(db);
        }
    }

    public TaskStorageAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    public TaskStorageAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }

    public long createTask(int line, String text) {

        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_LINE, line);
        initialValues.put(KEY_TEXT, text);

        return mDb.insert(SQLITE_TABLE, null, initialValues);
    }

    public boolean deleteAllTodos() {

        int doneDelete = 0;
        doneDelete = mDb.delete(SQLITE_TABLE, null, null);
        Log.w(TAG, Integer.toString(doneDelete));
        return doneDelete > 0;

    }

    public Cursor fetchTodoByText(String inputText) throws SQLException {
        Log.w(TAG, inputText);
        Cursor mCursor = null;
        if (inputText == null || inputText.length() == 0) {
            mCursor = mDb.query(SQLITE_TABLE, new String[]{KEY_ROWID,
                    KEY_LINE, KEY_TEXT},
                    null, null, null, null, null);

        } else {
            // TODO make this a prepared statement to prevent SQL injection
            mCursor = mDb.query(true, SQLITE_TABLE, new String[]{KEY_ROWID,
                    KEY_LINE, KEY_TEXT},
                    KEY_TEXT + " == '%" + inputText + "%'", null,
                    null, null, null, null);
        }
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;

    }

    public Cursor fetchAllTodos() {

        Cursor mCursor = mDb.query(SQLITE_TABLE, new String[]{KEY_ROWID,
                KEY_LINE, KEY_TEXT},
                null, null, null, null, null);

        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    public void insertSomeTasks() {


    }

}


