package nl.mpcjanssen.simpletask;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LogDbHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "log";
    public static final String LOG_TIMESTAMP = "severity";
    public static final String LOG_SEV = "severity";
    public static final String LOG_TAG = "tag";
    public static final String LOG_LINE = "line";
    public static final String LOG_EX = "exception";
    static final String TEXT_TYPE = " TEXT";
    static final String COMMA_SEP = ",";
    static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    LOG_TIMESTAMP + " " + TEXT_TYPE  +
                    LOG_SEV + TEXT_TYPE + COMMA_SEP +
                    LOG_TAG + TEXT_TYPE + COMMA_SEP +
                    LOG_LINE + TEXT_TYPE + COMMA_SEP +
                    LOG_EX + TEXT_TYPE + " )";

    static final String WHERE_AFTER_DATE =  LOG_TIMESTAMP + " < ?";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
// If you change the database schema, you must increment the database version.
public static final int DATABASE_VERSION = 1;
public static final String DATABASE_NAME = "TodoFiles_v" + DATABASE_VERSION + ".db";

    public LogDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database contains critical information, onUpgrade will not do anything
        // If the schema changes a new database is added
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}