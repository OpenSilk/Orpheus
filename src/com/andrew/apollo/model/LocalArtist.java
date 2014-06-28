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

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.opensilk.music.api.model.Artist;

/**
 * A class that represents an artist.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class LocalArtist extends Artist implements Parcelable {

    public final long artistId;

    public LocalArtist(long artistId, String name, int albumCount, int songCount) {
        super(String.valueOf(artistId), name, albumCount, songCount);
        this.artistId = artistId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalArtist)) return false;
        if (!super.equals(o)) return false;

        LocalArtist that = (LocalArtist) o;

        if (artistId != that.artistId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (artistId ^ (artistId >>> 32));
        return result;
    }

     /*
     * Implement Parcelable Interface
     */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(super.toBundle()); //cheating
        dest.writeLong(artistId);
    }

    public static final Creator<LocalArtist> CREATOR = new Creator<LocalArtist>() {
        @Override
        public LocalArtist createFromParcel(Parcel source) {
            Artist a = Artist.BUNDLE_CREATOR.fromBundle(source.readBundle());
            return new LocalArtist(source.readLong(), a.name, a.albumCount, a.songCount);
        }

        @Override
        public LocalArtist[] newArray(int size) {
            return new LocalArtist[size];
        }
    };
}
