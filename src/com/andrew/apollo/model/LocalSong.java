/*
 * Copyright (C) 2012 Andrew Neal Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.andrew.apollo.model;

import android.net.Uri;
import android.text.TextUtils;

import org.opensilk.music.api.model.Song;

/**
 * A class that represents a song.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class LocalSong extends Song {

    public final long songId;
    public final long albumId;

    public LocalSong(long songId, String name, String albumName, String artistName,
                     String albumArtistName, long albumId, int duration, Uri dataUri,
                     Uri artworkUri, String mimeType) {
        super(String.valueOf(songId), name, albumName, artistName,
                albumArtistName, String.valueOf(albumId), duration,
                dataUri, artworkUri, mimeType);
        this.songId = songId;
        this.albumId = albumId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalSong)) return false;
        if (!super.equals(o)) return false;

        LocalSong localSong = (LocalSong) o;

        if (albumId != localSong.albumId) return false;
        if (songId != localSong.songId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (songId ^ (songId >>> 32));
        result = 31 * result + (int) (albumId ^ (albumId >>> 32));
        return result;
    }

}
