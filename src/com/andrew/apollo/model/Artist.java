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

/**
 * A class that represents an artist.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Artist implements Parcelable {

    /**
     * The unique Id of the artist
     */
    public long mArtistId;

    /**
     * The artist name
     */
    public String mArtistName;

    /**
     * The number of albums for the artist
     */
    public int mAlbumNumber;

    /**
     * The number of songs for the artist
     */
    public int mSongNumber;

    /**
     * Constructor of <code>Artist</code>
     * 
     * @param artistId The Id of the artist
     * @param artistName The artist name
     * @param songNumber The number of songs for the artist
     * @param albumNumber The number of albums for the artist
     */
    public Artist(final long artistId, final String artistName, final int songNumber,
            final int albumNumber) {
        super();
        mArtistId = artistId;
        mArtistName = artistName;
        mSongNumber = songNumber;
        mAlbumNumber = albumNumber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + mAlbumNumber;
        result = prime * result + (int) mArtistId;
        result = prime * result + (mArtistName == null ? 0 : mArtistName.hashCode());
        result = prime * result + mSongNumber;
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Artist)) {
            return false;
        }
        final Artist other = (Artist)obj;
        if (mAlbumNumber != other.mAlbumNumber) {
            return false;
        }
        if (mArtistId != other.mArtistId) {
            return false;
        }
        if (!TextUtils.equals(mArtistName, other.mArtistName)) {
            return false;
        }
        if (mSongNumber != other.mSongNumber) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mArtistName;
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
        dest.writeLong(mArtistId);
        dest.writeString(mArtistName);
        dest.writeInt(mSongNumber);
        dest.writeInt(mAlbumNumber);
    }

    private Artist(Parcel in) {
        mArtistId = in.readLong();
        mArtistName = in.readString();
        mSongNumber = in.readInt();
        mAlbumNumber = in.readInt();
    }

    public static final Creator<Artist> CREATOR = new Creator<Artist>() {
        @Override
        public Artist createFromParcel(Parcel source) {
            return new Artist(source);
        }

        @Override
        public Artist[] newArray(int size) {
            return new Artist[size];
        }
    };
}
