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

package com.andrew.apollo.model;

import android.net.Uri;

import org.opensilk.music.api.model.Song;

/**
 * Created by drew on 6/26/14.
 */
public class RecentSong extends Song {

    public final long recentId;
    public final boolean isLocal;
    public final int playCount;
    public final long lastPlayed;

    public RecentSong(String identity, String name, String albumName, String artistName,
                      String albumArtistName, String albumIdentity, int duration,
                      Uri dataUri, Uri artworkUri, String mimeType, long recentId,
                      boolean isLocal, int playCount, long lastPlayed) {
        super(identity, name, albumName, artistName,
                albumArtistName, albumIdentity, duration, dataUri, artworkUri, mimeType);
        this.recentId = recentId;
        this.isLocal = isLocal;
        this.playCount = playCount;
        this.lastPlayed = lastPlayed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecentSong)) return false;
        if (!super.equals(o)) return false;

        RecentSong that = (RecentSong) o;

        if (isLocal != that.isLocal) return false;
        if (lastPlayed != that.lastPlayed) return false;
        if (playCount != that.playCount) return false;
        if (recentId != that.recentId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (recentId ^ (recentId >>> 32));
        result = 31 * result + (isLocal ? 1 : 0);
        result = 31 * result + playCount;
        result = 31 * result + (int) (lastPlayed ^ (lastPlayed >>> 32));
        return result;
    }
}
