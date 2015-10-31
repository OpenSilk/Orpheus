/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.library.drive.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import org.opensilk.common.core.dagger2.ForApplication;

import javax.inject.Inject;

/**
 * Created by drew on 10/21/15.
 */
@DriveLibraryProviderScope
public class DriveLibraryDB extends SQLiteOpenHelper {

    static final String DB_NAME = "driveaccounts.db";
    static final int DB_VERSION = 1;

    @Inject
    public DriveLibraryDB(@ForApplication Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        onUpgrade(db, 0, DB_VERSION);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion <= DB_VERSION) {
            db.execSQL("CREATE TABLE IF NOT EXISTS account (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "account VARCHAR(128) NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                    "invalid INTEGER NOT NULL DEFAULT 0 " +
                    ");");
            db.execSQL("CREATE TABLE IF NOT EXISTS bookmark (" +
                    "_id INTEGER PRIMARY KEY, " +
                    "folder_uri TEXT NOT NULL UNIQUE ON CONFLICT REPLACE, " +
                    "parent_uri TEXT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "account_id INTEGER REFERENCES account(_id) ON DELETE CASCADE ON UPDATE CASCADE " +
                    ");");
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.execSQL("PRAGMA foreign_keys = ON;");
//        db.execSQL("PRAGMA encoding = 'UTF-8';");
    }



    interface SCHEMA {
        interface ACCOUNT extends BaseColumns {
            String TABLE = "account";
            String ACCOUNT = "account";
            String INVALID = "invalid";
        }
        interface BOOKMARK extends BaseColumns {
            String TABLE = "bookmark";
            String FOLDER_URI = "folder_uri";
            String PARENT_URI = "parent_uri";
            String TITLE = "title";
            String ACCOUNT_ID = "account_id";
        }
    }
}
