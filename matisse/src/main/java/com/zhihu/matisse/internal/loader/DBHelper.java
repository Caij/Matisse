package com.zhihu.matisse.internal.loader;

import android.content.Context;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "album_info_" + Build.VERSION.SDK_INT;
    private static SQLiteDatabase database;

    public DBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `AppAlbum` (`id` TEXT NOT NULL,  `displayName` TEXT, PRIMARY KEY(`id`))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public static synchronized SQLiteDatabase getSQLiteDatabase(Context context) {
        if (database == null) {
            DBHelper dbHelper = new DBHelper(context);
            database = dbHelper.getWritableDatabase();
        }
        return database;
    }
}
