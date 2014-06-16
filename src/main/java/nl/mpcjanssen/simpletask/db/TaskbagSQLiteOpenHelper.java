package nl.mpcjanssen.simpletask.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TaskbagSQLiteOpenHelper extends SQLiteOpenHelper {
    public static final String TABLE_TASKBAG = "taskbag";
    private static final String DATABASE_NAME = "taskbag.db";
    private static final int DATABASE_VERSION = 1;
    
    public TaskbagSQLiteOpenHelper(Context context) {
	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase database) {
	
    }

    @Override
    public void onUpgrade (SQLiteDatabase db, int oldVersion, int newVersion) {
	
    }
}
