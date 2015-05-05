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

    public interface Q {
        String Q = "q";
        String FOLDERS_ONLY = "foldersOnly";
        String TRACKS_ONLY = "tracksOnly";
    }

    static final String scheme = "content";
    static final String model = "model";
    static final String albums = "albums";
    static final String album = "album";
    static final String artists = "artists";
    static final String artist = "artist";
    static final String folders = "folders";
    static final String folder = "folder";
    static final String genres = "genres";
    static final String genre = "genre";
    static final String playlists = "playlists";
    static final String playlist = "playlist";
    static final String tracks = "tracks";
    static final String track = "track";

    private static Uri.Builder modelBase(String authority, String library){
        return new Uri.Builder().scheme(scheme).authority(authority).appendPath(library).appendPath(model);
    }

    public static Uri withQ(Uri uri, String q) {
        return uri.buildUpon().appendQueryParameter(Q.Q, q).build();
    }

    public static Uri albums(String authority, String library) {
        return album(authority, library, null);
    }

    public static Uri album(String authority, String library, String id) {
        if (id == null) {
            return modelBase(authority, library).appendPath(albums).build();
        }
        return modelBase(authority, library).appendPath(album).appendPath(id).build();
    }

    public static Uri albumTracks(String authority, String library, String id) {
        return modelBase(authority, library).appendPath(album).appendPath(id).appendPath(tracks).build();
    }

    public static Uri artists(String authority, String library) {
        return artist(authority, library, null);
    }

    public static Uri artist(String authority, String library, String id) {
        if (id == null) {
            return modelBase(authority, library).appendPath(artists).build();
        }
        return modelBase(authority, library).appendPath(artist).appendPath(id).build();
    }

    public static Uri artistAlbums(String authority, String library, String id) {
        return modelBase(authority, library).appendPath(artist).appendPath(id).appendPath(albums).build();
    }

    public static Uri artistTracks(String authority, String library, String id) {
        return modelBase(authority, library).appendPath(artist).appendPath(id).appendPath(tracks).build();
    }

    public static Uri folders(String authority, String library) {
        return folders(authority, library, null);
    }

    public static Uri folders(String authority, String library, String id) {
        if (id == null) {
            return modelBase(authority, library).appendPath(folders).build();
        }
        return modelBase(authority, library).appendPath(folder).appendPath(id).build();
    }

    /**
     * @return Uri with query for children of type {@link org.opensilk.music.model.Folder}
     */
    public static Uri folderFolders(String authority, String library, String id) {
        return withQ(folders(authority, library, id), Q.FOLDERS_ONLY);
    }

    /**
     * @return Uri with query for children of type {@link org.opensilk.music.model.Track}
     */
    public static Uri folderTracks(String authority, String library, String id) {
        return withQ(folders(authority, library, id), Q.TRACKS_ONLY);
    }

    public static Uri genres(String authority, String library) {
        return genre(authority, library, null);
    }

    public static Uri genre(String authority, String library, String id) {
        if (id == null) {
            return modelBase(authority, library).appendPath(genres).build();
        }
        return modelBase(authority, library).appendPath(genre).appendPath(id).build();
    }

    public static Uri genreAlbums(String authority, String library, String id) {
        return modelBase(authority, library).appendPath(genre).appendPath(id).appendPath(albums).build();
    }

    public static Uri genreTracks(String authority, String library, String id) {
        return modelBase(authority, library).appendPath(genre).appendPath(id).appendPath(tracks).build();
    }

    public static Uri playlists(String authority, String library) {
        return playlist(authority, library, null);
    }

    public static Uri playlist(String authority, String library, String id) {
        if (id == null) {
            return modelBase(authority, library).appendPath(playlists).build();
        }
        return modelBase(authority, library).appendPath(playlist).appendPath(id).build();
    }

    public static Uri playlistTracks(String authority, String library, String id) {
        return modelBase(authority, library).appendPath(playlist).appendPath(id).appendPath(tracks).build();
    }

    public static Uri tracks(String authority, String library) {
        return track(authority, library, null);
    }

    public static Uri track(String authority, String library, String id) {
        if (id == null) {
            return modelBase(authority, library).appendPath(tracks).build();
        }
        return modelBase(authority, library).appendPath(track).appendPath(id).build();
    }

    public static Uri call(String authority) {
        return new Uri.Builder().scheme(scheme).authority(authority).build();
    }

    public static final int M_ALBUMS = 1;
    public static final int M_ALBUM = 2;
    public static final int M_ARTISTS = 3;
    public static final int M_ARTIST = 4;
    public static final int M_FOLDERS = 5;
    public static final int M_FOLDER = 6;
    public static final int M_GENRES = 7;
    public static final int M_GENRE = 8;
    public static final int M_PLAYLISTS = 9;
    public static final int M_PLAYLIST = 10;
    public static final int M_TRACKS = 11;
    public static final int M_TRACK = 12;
    public static final int M_ALBUM_TRACKS = 13;
    public static final int M_ARTIST_ALBUMS = 14;
    public static final int M_ARTIST_TRACKS = 15;
    public static final int M_GENRE_ALBUMS = 16;
    public static final int M_GENRE_TRACKS = 17;
    public static final int M_PLAYLIST_TRACKS = 18;

    private static final String slash_wild = "/*";
    private static final String slash_wild_slash = "/*/";
    private static final String model_base_match = "*/" + model + "/";

    public static UriMatcher makeMatcher(String authority) {
        Log.i("LibraryUris", "Creating matcher for authority="+authority);
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(authority, model_base_match + albums, M_ALBUMS);
        uriMatcher.addURI(authority, model_base_match + album + slash_wild, M_ALBUM);
        uriMatcher.addURI(authority, model_base_match + album + slash_wild_slash + tracks, M_ALBUM_TRACKS);

        uriMatcher.addURI(authority, model_base_match + artists, M_ARTISTS);
        uriMatcher.addURI(authority, model_base_match + artist + slash_wild, M_ARTIST);
        uriMatcher.addURI(authority, model_base_match + artist + slash_wild_slash + albums, M_ARTIST_ALBUMS);
        uriMatcher.addURI(authority, model_base_match + artist + slash_wild_slash + tracks, M_ARTIST_TRACKS);

        uriMatcher.addURI(authority, model_base_match + folders, M_FOLDERS);
        uriMatcher.addURI(authority, model_base_match + folder + slash_wild, M_FOLDER);

        uriMatcher.addURI(authority, model_base_match + genres, M_GENRES);
        uriMatcher.addURI(authority, model_base_match + genre + slash_wild, M_GENRE);
        uriMatcher.addURI(authority, model_base_match + genre + slash_wild_slash + albums, M_GENRE_ALBUMS);
        uriMatcher.addURI(authority, model_base_match + genre + slash_wild_slash + tracks, M_GENRE_TRACKS);

        uriMatcher.addURI(authority, model_base_match + playlists, M_PLAYLISTS);
        uriMatcher.addURI(authority, model_base_match + playlist + slash_wild, M_PLAYLIST);
        uriMatcher.addURI(authority, model_base_match + playlist + slash_wild_slash + tracks, M_PLAYLIST_TRACKS);

        uriMatcher.addURI(authority, model_base_match + tracks, M_TRACKS);
        uriMatcher.addURI(authority, model_base_match + track + slash_wild, M_TRACK);

        return uriMatcher;
    }

}
