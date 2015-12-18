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

import android.content.ContentUris;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by drew on 7/16/14.
 */
public class Uris {
    public static final Uri ARTWORK_URI = Uri.parse("content://media/external/audio/albumart");
    public static final Uri EXTERNAL_MEDIASTORE_MEDIA = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    public static final Uri EXTERNAL_MEDIASTORE_ALBUMS = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    public static final Uri EXTERNAL_MEDIASTORE_ARTISTS = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    public static Uri EXTERNAL_MEDIASTORE_ARTISTS_ALBUMS(String artistId) {
        return MediaStore.Audio.Artists.Albums.getContentUri("external", Long.valueOf(artistId));
    }
    public static Uri PLAYLIST_MEMBERS(String playlistId) {
        return  MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(playlistId));
    }
    public static final Uri EXTERNAL_MEDIASTORE_PLAYLISTS = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
    public static Uri PLAYLIST(String id) {
        return ContentUris.withAppendedId(EXTERNAL_MEDIASTORE_PLAYLISTS, Long.valueOf(id));
    }
    public static final Uri EXTERNAL_MEDIASTORE_GENRES = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
    public static Uri GENRE_MEMBERS(String genreId) {
        return MediaStore.Audio.Genres.Members.getContentUri("external", Long.valueOf(genreId));
    }
    public static final Uri EXTERNAL_MEDIASTORE_FILES = MediaStore.Files.getContentUri("external");
}
