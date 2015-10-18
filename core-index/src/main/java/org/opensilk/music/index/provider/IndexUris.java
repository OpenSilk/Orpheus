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

package org.opensilk.music.index.provider;

import android.content.UriMatcher;
import android.net.Uri;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Genre;

import timber.log.Timber;

/**
 * Created by drew on 4/26/15.
 */
public class IndexUris {

    static final String scheme = "content";
    static final String model = "model";
    static final String albums = "albums";
    static final String album = "album";
    static final String artists = "artists";
    static final String artist = "artist";
    static final String genres = "genres";
    static final String genre = "genre";
    static final String playlists = "playlists";
    static final String playlist = "playlist";
    static final String tracks = "tracks";
    static final String track = "track";
    static final String details = "details";
    static final String locations = "locations";
    static final String albumArtists = "albumArtists";
    static final String artistBio = "artistBio";
    static final String albumBio = "albumBio";

    private static Uri.Builder baseUriBuilder(String authority) {
        return new Uri.Builder().scheme(scheme).authority(authority);
    }

    private static Uri.Builder modelBase(String authority){
        return baseUriBuilder(authority).appendPath(model);
    }

    private static Uri.Builder detailsBase(String authority) {
        return modelBase(authority).appendPath(details);
    }

    public static Uri albums(String authority) {
        return modelBase(authority).appendPath(albums).build();
    }

    public static Uri album(String authority, String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Null id");
        }
        return modelBase(authority).appendPath(album).appendPath(id).build();
    }

    public static Uri albumTracks(String authority, String id) {
        return modelBase(authority).appendPath(album).appendPath(id).appendPath(tracks).build();
    }

    public static Uri albumDetails(Album a) {
        String authority = a.getUri().getAuthority();
        String id = a.getUri().getLastPathSegment();
        return albumDetails(authority, id);
    }

    public static Uri albumDetails(String authority, String id) {
        return modelBase(authority).appendPath(album).appendPath(id).appendPath(details).build();
    }

    public static Uri artists(String authority) {
        return modelBase(authority).appendPath(artists).build();
    }

    public static Uri albumArtists(String authority) {
        return modelBase(authority).appendPath(albumArtists).build();
    }

    public static Uri artist(String authority, String id) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Null id");
        }
        return modelBase(authority).appendPath(artist).appendPath(id).build();
    }

    public static Uri artistAlbums(String authority, String id) {
        return modelBase(authority).appendPath(artist).appendPath(id).appendPath(albums).build();
    }

    public static Uri artistTracks(String authority, String id) {
        return modelBase(authority).appendPath(artist).appendPath(id).appendPath(tracks).build();
    }

    public static Uri artistDetails(Artist a) {
        final String authority = a.getUri().getAuthority();
        final String id = a.getUri().getLastPathSegment();
        return artistDetails(authority, id);
    }

    public static Uri artistDetails(String authority, String id) {
        return modelBase(authority).appendPath(artist).appendPath(id).appendPath(details).build();
    }

    public static Uri genres(String authority) {
        return genre(authority, null);
    }

    public static Uri genre(String authority, String id) {
        if (StringUtils.isEmpty(id)) {
            return modelBase(authority).appendPath(genres).build();
        }
        return modelBase(authority).appendPath(genre).appendPath(id).build();
    }

    public static Uri genreAlbums(String authority, String id) {
        return modelBase(authority).appendPath(genre).appendPath(id).appendPath(albums).build();
    }

    public static Uri genreTracks(String authority, String id) {
        return modelBase(authority).appendPath(genre).appendPath(id).appendPath(tracks).build();
    }

    public static Uri genreDetails(Genre a) {
        final String authority = a.getUri().getAuthority();
        final String id = a.getUri().getLastPathSegment();
        return genreDetails(authority, id);
    }

    public static Uri genreDetails(String authority, String id) {
        return modelBase(authority).appendPath(genre).appendPath(id).appendPath(details).build();
    }

    public static Uri playlists(String authority) {
        return playlist(authority, null);
    }

    public static Uri playlist(String authority, String id) {
        if (StringUtils.isEmpty(id)) {
            return modelBase(authority).appendPath(playlists).build();
        }
        return modelBase(authority).appendPath(playlist).appendPath(id).build();
    }

    public static Uri playlistTracks(String authority, String id) {
        return modelBase(authority).appendPath(playlist).appendPath(id).appendPath(tracks).build();
    }

    public static Uri tracks(String authority) {
        return track(authority, null);
    }

    public static Uri track(String authority, String id) {
        if (StringUtils.isEmpty(id)) {
            return modelBase(authority).appendPath(tracks).build();
        }
        return modelBase(authority).appendPath(track).appendPath(id).build();
    }

    public static Uri locations(String authority) {
        return modelBase(authority).appendPath(locations).build();
    }

    public static Uri artistBio(String authority, String id) {
        return modelBase(authority).appendPath(artistBio).appendPath(id).build();
    }

    public static Uri albumBio(String authority, String id) {
        return modelBase(authority).appendPath(albumBio).appendPath(id).build();
    }

    public static Uri call(String authority) {
        return baseUriBuilder(authority).build();
    }

    public static final int M_ALBUMS = 1;
    public static final int M_ALBUM = 2;
    public static final int M_ARTISTS = 3;
    public static final int M_ARTIST = 4;
//    public static final int M_FOLDERS = 5;
//    public static final int M_FOLDER = 6;
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
    public static final int M_ALBUM_DETAILS = 19;
    public static final int M_ARTIST_DETAILS = 20;
    public static final int M_GENRE_DETAILS = 21;
    public static final int M_LOCATIONS = 22;
    public static final int M_ALBUM_ARTISTS = 23;
    public static final int M_ARTIST_BIO = 24;
    public static final int M_ALBUM_BIO = 25;

    private static final String slash_wild = "/*";
    private static final String slash_wild_slash = "/*/";
    private static final String base_match = "*/";
    private static final String model_base_match = model + "/";

    public static UriMatcher makeMatcher(String authority) {
        Timber.i("Creating matcher for authority=%s", authority);
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        uriMatcher.addURI(authority, model_base_match + albums, M_ALBUMS);
        uriMatcher.addURI(authority, model_base_match + album + slash_wild, M_ALBUM);
        uriMatcher.addURI(authority, model_base_match + album + slash_wild_slash + tracks, M_ALBUM_TRACKS);
        uriMatcher.addURI(authority, model_base_match + album + slash_wild_slash + details, M_ALBUM_DETAILS);

        uriMatcher.addURI(authority, model_base_match + artists, M_ARTISTS);
        uriMatcher.addURI(authority, model_base_match + albumArtists, M_ALBUM_ARTISTS);
        uriMatcher.addURI(authority, model_base_match + artist + slash_wild, M_ARTIST);
        uriMatcher.addURI(authority, model_base_match + artist + slash_wild_slash + albums, M_ARTIST_ALBUMS);
        uriMatcher.addURI(authority, model_base_match + artist + slash_wild_slash + tracks, M_ARTIST_TRACKS);
        uriMatcher.addURI(authority, model_base_match + artist + slash_wild_slash + details, M_ARTIST_DETAILS);

        uriMatcher.addURI(authority, model_base_match + genres, M_GENRES);
        uriMatcher.addURI(authority, model_base_match + genre + slash_wild, M_GENRE);
        uriMatcher.addURI(authority, model_base_match + genre + slash_wild_slash + albums, M_GENRE_ALBUMS);
        uriMatcher.addURI(authority, model_base_match + genre + slash_wild_slash + tracks, M_GENRE_TRACKS);
        uriMatcher.addURI(authority, model_base_match + genre + slash_wild_slash + details, M_GENRE_DETAILS);

        uriMatcher.addURI(authority, model_base_match + playlists, M_PLAYLISTS);
        uriMatcher.addURI(authority, model_base_match + playlist + slash_wild, M_PLAYLIST);
        uriMatcher.addURI(authority, model_base_match + playlist + slash_wild_slash + tracks, M_PLAYLIST_TRACKS);

        uriMatcher.addURI(authority, model_base_match + tracks, M_TRACKS);
        uriMatcher.addURI(authority, model_base_match + track + slash_wild, M_TRACK);

        uriMatcher.addURI(authority, model_base_match + locations, M_LOCATIONS);

        uriMatcher.addURI(authority, model_base_match + albumBio + slash_wild, M_ALBUM_BIO);
        uriMatcher.addURI(authority, model_base_match + artistBio + slash_wild, M_ARTIST_BIO);

        return uriMatcher;
    }

}
