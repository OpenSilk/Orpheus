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
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.opensilk.music.api.model.Album;

/**
 * A class that represents an album.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class LocalAlbum extends Album implements Parcelable {

    public final long albumId;

    public LocalAlbum(long albumId, String name, String artistName, int songCount, String date, Uri artworkUri) {
        super(String.valueOf(albumId), name, artistName, songCount, date, artworkUri);
        this.albumId = albumId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LocalAlbum)) return false;
        if (!super.equals(o)) return false;

        LocalAlbum that = (LocalAlbum) o;

        if (albumId != that.albumId) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (int) (albumId ^ (albumId >>> 32));
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
        dest.writeLong(albumId);
    }

    public static final Creator<LocalAlbum> CREATOR = new Creator<LocalAlbum>() {
        @Override
        public LocalAlbum createFromParcel(Parcel source) {
            Album a = Album.BUNDLE_CREATOR.fromBundle(source.readBundle());
            return new LocalAlbum(source.readLong(), a.name, a.artistName, a.songCount, a.date, a.artworkUri);
        }

        @Override
        public LocalAlbum[] newArray(int size) {
            return new LocalAlbum[size];
        }
    };

}
