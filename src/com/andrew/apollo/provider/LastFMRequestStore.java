package com.andrew.apollo.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by drew on 3/11/14.
 */
public class LastFMRequestStore extends SQLiteOpenHelper {

    private static final int VERSION = 1;

    public static final String DATABASE_NAME = "lastfmstore.db";

    public static final String TABLE_NAME = "LASTFM_CACHE";

    private static LastFMRequestStore sInstance = null;

    public static synchronized LastFMRequestStore getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new LastFMRequestStore(context);
        }
        return sInstance;
    }

    private LastFMRequestStore(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (id VARCHAR(200) PRIMARY KEY, expiration_date LONG, response TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
