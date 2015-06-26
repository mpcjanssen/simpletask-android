package nl.mpcjanssen.simpletask;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.ConcurrentMap;

public class BackupDbHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "todofiles";
    public static final String FILE_ID = "contents";
    public static final String FILE_NAME = "name";
    public static final String FILE_DATE = "date";
    static final String TEXT_TYPE = " TEXT";
    static final String COMMA_SEP = ",";
    static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    FILE_ID + " " + TEXT_TYPE + " UNIQUE PRIMARY KEY," +
                    FILE_NAME + TEXT_TYPE + COMMA_SEP +
                    FILE_DATE + TEXT_TYPE+ " )";

    static final String WHERE_AFTER_DATE =  FILE_DATE + " < ?";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + TABLE_NAME;
// If you change the database schema, you must increment the database version.
public static final int DATABASE_VERSION = 2;
public static final String DATABASE_NAME = "TodoFiles.db";

    public BackupDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database contains critical information, onUpgrade should usually not do anything
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}