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

package org.opensilk.music.library.mediastore.provider;

import android.content.UriMatcher;
import android.net.Uri;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import timber.log.Timber;

/**
 * Created by drew on 9/9/15.
 */
public class FoldersUris {

    static final String scheme = "content";
    static final String folders = "folders";
    static final String folder = "folder";
    static final String tracks = "tracks";
    static final String track = "track";
    static final String playlists = "playlists";
    static final String playlist = "playlist";
    static final String external = "external";

    private static Uri.Builder baseUriBuilder(String authority, String library) {
        return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library);
    }

    public static Uri folders(String authority, String library) {
        return baseUriBuilder(authority, library).appendPath(folders).build();
    }

    public static Uri folder(String authority, String library, String id) {
        if (StringUtils.isEmpty(id)) {
            return folders(authority, library);
        }
        return baseUriBuilder(authority, library).appendPath(folder).appendPath(id).build();
    }

    public static Uri tracks(String authority, String library) {
        return baseUriBuilder(authority, library).appendPath(tracks).build();
    }

    public static Uri track(String authority, String library, String id) {
        if (StringUtils.isEmpty(id)) {
            return tracks(authority, library);
        }
        return baseUriBuilder(authority, library).appendPath(track).appendPath(id).build();
    }

    public static Uri track(String authority, int volumeId, long trackId) {
        return track(authority, String.valueOf(volumeId), String.valueOf(trackId));
    }

    public static Uri playlists(String authority) {
        return baseUriBuilder(authority, external).appendPath(playlists).build();
    }

    public static Uri playlist(String authority, String id) {
        return baseUriBuilder(authority, external).appendPath(playlist).appendPath(id).build();
    }

    public static Uri playlistTracks(String authority, String id) {
        return baseUriBuilder(authority, external).appendPath(playlist).appendPath(id).appendPath(tracks).build();
    }

    public static final int M_FOLDERS = 5;
    public static final int M_FOLDER = 6;

    public static final int M_TRACKS = 11;
    public static final int M_TRACK_PTH = 12;
    public static final int M_TRACK_MS = 13;
    public static final int M_PLAYLISTS = 14;
    public static final int M_PLAYLIST = 15;
    public static final int M_PLAYLIST_TRACKS = 16;

    private static final String slash_wild = "/*";
    private static final String slash_num = "/#";
    private static final String base_match = "#/";
    private static final String external_slash = external + "/";
    private static final String slash_num_slash = "/#/";

    public static UriMatcher makeMatcher(String authority) {
        Timber.i("Creating matcher for authority=%s", authority);
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(authority, base_match + folders, M_FOLDERS);
        uriMatcher.addURI(authority, base_match + folder + slash_wild, M_FOLDER);

        uriMatcher.addURI(authority, base_match + tracks, M_TRACKS);
        uriMatcher.addURI(authority, base_match + track + slash_wild, M_TRACK_PTH);
        uriMatcher.addURI(authority, base_match + track + slash_num, M_TRACK_MS);

        uriMatcher.addURI(authority, external_slash + playlists, M_PLAYLISTS);
        uriMatcher.addURI(authority, external_slash + playlist + slash_num, M_PLAYLIST);
        uriMatcher.addURI(authority, external_slash + playlist + slash_num_slash + tracks, M_PLAYLIST_TRACKS);

        return uriMatcher;
    }
}
