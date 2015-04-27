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

import static org.opensilk.music.library.provider.LibraryProvider.AUTHORITY;

/**
 * Created by drew on 4/26/15.
 */
public class Uris {

    public static final Uri FOLDERS;
    public static final Uri ALBUMS;
    public static final Uri ARTISTS;
    public static final Uri TRACKS;

    public static Uri folder(String id) {
        return FOLDER.buildUpon().appendPath(id).build();
    }

    public static Uri album(String id) {
        return ALBUM.buildUpon().appendPath(id).build();
    }

    public static Uri artist(String id) {
        return ARTIST.buildUpon().appendPath(id).build();
    }

    public static Uri track(String id) {
        return TRACK.buildUpon().appendPath(id).build();
    }

    static final Uri FOLDER;
    static final Uri ALBUM;
    static final Uri ARTIST;
    static final Uri TRACK;

    static final int M_FOLDERS = 1;
    static final int M_FOLDER = 2;
    static final int M_ALBUMS = 3;
    static final int M_ALBUM = 4;
    static final int M_ARTISTS = 5;
    static final int M_ARTIST = 6;
    static final int M_TRACKS = 7;
    static final int M_TRACK = 8;

    static final UriMatcher MATCHER;

    static {
        final String scheme = "content";
        MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

        final String folders = "folders";
        FOLDERS = new Uri.Builder().scheme(scheme).authority(AUTHORITY).appendPath(folders).build();
        MATCHER.addURI(AUTHORITY, folders, M_FOLDERS);

        final String folder = "folder";
        FOLDER = new Uri.Builder().scheme(scheme).authority(AUTHORITY).appendPath(folder).build();
        MATCHER.addURI(AUTHORITY, folder + "/*", M_FOLDER);

        final String albums = "albums";
        ALBUMS = new Uri.Builder().scheme(scheme).authority(AUTHORITY).appendPath(albums).build();
        MATCHER.addURI(AUTHORITY, albums, M_ALBUMS);

        final String album = "album";
        ALBUM = new Uri.Builder().scheme(scheme).authority(AUTHORITY).appendPath(album).build();
        MATCHER.addURI(AUTHORITY, album + "/*", M_ALBUM);

        final String artists = "artists";
        ARTISTS = new Uri.Builder().scheme(scheme).authority(AUTHORITY).appendPath(artists).build();
        MATCHER.addURI(AUTHORITY, artists, M_ARTISTS);

        final String artist = "artist";
        ARTIST = new Uri.Builder().scheme(scheme).authority(AUTHORITY).appendPath(artist).build();
        MATCHER.addURI(AUTHORITY, artist + "/*", M_ARTIST);

        final String tracks = "tracks";
        TRACKS = new Uri.Builder().scheme(scheme).authority(AUTHORITY).appendPath(tracks).build();
        MATCHER.addURI(AUTHORITY, tracks, M_TRACKS);

        final String track = "track";
        TRACK = new Uri.Builder().scheme(scheme).authority(AUTHORITY).appendPath(track).build();
        MATCHER.addURI(AUTHORITY, track + "/*", M_TRACK);
    }

}
