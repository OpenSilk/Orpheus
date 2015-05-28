/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.library.mediastore.util;

import android.provider.BaseColumns;
import android.provider.MediaStore;

/**
 * Created by drew on 2/22/14.
 */
public interface Projections {

    String[] ID_ONLY = new String[] {
                BaseColumns._ID,
        };
    String[] ID_DATA = new String[] {
            BaseColumns._ID,
            MediaStore.Audio.AudioColumns.DATA
    };
    String[] LOCAL_SONG = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.AudioColumns.TITLE,
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                "album_artist",
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.MIME_TYPE,
                MediaStore.Audio.AudioColumns.TRACK,
        };
    String [] SONG_FILE = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.AudioColumns.DISPLAY_NAME, //Better sorting than TITLE
                MediaStore.Audio.AudioColumns.ARTIST,
                MediaStore.Audio.AudioColumns.ALBUM,
                MediaStore.Audio.AudioColumns.ALBUM_ID,
                "album_artist",
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.MIME_TYPE,
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.AudioColumns.TRACK,
    };
    String[] LOCAL_ALBUM = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.AlbumColumns.ALBUM,
                MediaStore.Audio.AlbumColumns.ARTIST,
                MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS,
                MediaStore.Audio.AlbumColumns.FIRST_YEAR,
                MediaStore.Audio.AlbumColumns.LAST_YEAR,
        };
    String[] LOCAL_ARTIST = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.ArtistColumns.ARTIST,
                MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS,
                MediaStore.Audio.ArtistColumns.NUMBER_OF_TRACKS,
        };
    String[] PLAYLIST_SONGS = new String[] {
            MediaStore.Audio.Playlists.Members.AUDIO_ID + " AS " + BaseColumns._ID,
            MediaStore.Audio.AudioColumns.TITLE,
            MediaStore.Audio.AudioColumns.ARTIST,
            MediaStore.Audio.AudioColumns.ALBUM,
            MediaStore.Audio.AudioColumns.ALBUM_ID,
            "album_artist",
            MediaStore.Audio.AudioColumns.DURATION,
            MediaStore.Audio.AudioColumns.MIME_TYPE,
            MediaStore.Audio.Playlists.Members.PLAY_ORDER + " AS " + MediaStore.Audio.AudioColumns.TRACK,
        };
    String[] GENRE_SONGS = new String[] {
            MediaStore.Audio.Genres.Members.AUDIO_ID + " AS " + BaseColumns._ID,
            MediaStore.Audio.AudioColumns.TITLE,
            MediaStore.Audio.AudioColumns.ARTIST,
            MediaStore.Audio.AudioColumns.ALBUM,
            MediaStore.Audio.AudioColumns.ALBUM_ID,
            "album_artist",
            MediaStore.Audio.AudioColumns.DURATION,
            MediaStore.Audio.AudioColumns.MIME_TYPE,
            MediaStore.Audio.AudioColumns.TRACK,
        };
    String[] GENRE = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.Genres.NAME,
        };
    String[] GENRE_MEMBER = new String[] {
                MediaStore.Audio.Genres.Members.AUDIO_ID,
                MediaStore.Audio.Genres.Members.ALBUM_ID,
        };
    String[] PLAYLIST = new String[] {
                BaseColumns._ID,
                MediaStore.Audio.Playlists.NAME,
        };
    String[] PLAYLIST_MEMBER = new String[] {
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.ALBUM_ID,
        };
    String[] MEDIA_TYPE_PROJECTION = new String[] {
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Audio.AudioColumns.DATA
    };

}
