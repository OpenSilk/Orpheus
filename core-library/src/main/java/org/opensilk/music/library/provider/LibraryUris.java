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

package org.opensilk.music.library.provider;

import android.content.UriMatcher;
import android.net.Uri;
import android.util.Log;

/**
 * Created by drew on 4/26/15.
 */
public class LibraryUris {

    public static final String QUERY_FOLDERS_ONLY = "foldersOnly";

    static final String scheme = "content";
    static final String albums = "albums";
    static final String album = "album";
    static final String artists = "artists";
    static final String artist = "artist";
    static final String folders = "folders";
    static final String folder = "folder";
    static final String tracks = "tracks";
    static final String track = "track";

    public static Uri albums(String authority, String library) {
        return album(authority, library, null);
    }

    public static Uri album(String authority, String library, String id) {
        if (id == null) {
            return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(albums).build();
        }
        return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(album).appendPath(id).build();
    }

    public static Uri artists(String authority, String library) {
        return artist(authority, library, null);
    }

    public static Uri artist(String authority, String library, String id) {
        if (id == null) {
            return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(artists).build();
        }
        return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(artist).appendPath(id).build();
    }

    public static Uri folders(String authority, String library) {
        return foldersTracks(authority, library).buildUpon().appendQueryParameter(QUERY_FOLDERS_ONLY, "true").build();
    }

    public static Uri folders(String authority, String library, String id) {
        return foldersTracks(authority, library, id).buildUpon().appendQueryParameter(QUERY_FOLDERS_ONLY, "true").build();
    }

    public static Uri foldersTracks(String authority, String library) {
        return foldersTracks(authority, library, null);
    }

    public static Uri foldersTracks(String authority, String library, String id) {
        if (id == null) {
            return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(folders).build();
        }
        return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(folder).appendPath(id).build();
    }

    public static Uri tracks(String authority, String library) {
        return track(authority, library, null);
    }

    public static Uri track(String authority, String library, String id) {
        if (id == null) {
            return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(tracks).build();
        }
        return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(track).appendPath(id).build();
    }

    public static Uri call(String authority) {
        return new Uri.Builder().scheme(scheme).authority(authority).build();
    }

    static final int M_ALBUMS = 1;
    static final int M_ALBUM = 2;
    static final int M_ARTISTS = 3;
    static final int M_ARTIST = 4;
    static final int M_FOLDERS = 5;
    static final int M_FOLDER = 6;
    static final int M_TRACKS = 7;
    static final int M_TRACK = 8;

    static UriMatcher makeMatcher(String authority) {
        Log.i("Uris", "Creating matcher for authority="+authority);
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(authority, "*/" + albums, M_ALBUMS);
        uriMatcher.addURI(authority, "*/" + album + "/*", M_ALBUM);

        uriMatcher.addURI(authority, "*/" + artists, M_ARTISTS);
        uriMatcher.addURI(authority, "*/" + artist + "/*", M_ARTIST);

        uriMatcher.addURI(authority, "*/" + folders, M_FOLDERS);
        uriMatcher.addURI(authority, "*/" + folder + "/*", M_FOLDER);

        uriMatcher.addURI(authority, "*/" + tracks, M_TRACKS);
        uriMatcher.addURI(authority, "*/" + track + "/*", M_TRACK);

        return uriMatcher;
    }

}
