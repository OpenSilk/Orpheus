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

package org.opensilk.music.index.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Track;

import java.util.List;

/**
 * Created by drew on 9/13/15.
 */
public interface IndexDatabase {
    List<Artist> getArtists(String sortOrder);
    List<Album> getAlbums(String sortOrder);
    List<Track> getTracks(String sortOrder, boolean excludeOrphaned);
    Cursor query(String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy);
    int delete(String table, String whereClause, String[] whereArgs);
    long insert(String table, String nullColumnHack, ContentValues values,int conflictAlgorithm);
    int update(String table, ContentValues values, String whereClause, String[] whereArgs);
    boolean hasContainer(Uri uri);
    void addContainer(Uri uri);
}
