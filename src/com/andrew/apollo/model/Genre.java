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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that represents a genre.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Genre implements Parcelable {

    /**
     * The unique Id of the genre
     */
    public long mGenreId;

    /**
     * The genre name
     */
    public String mGenreName;

    /**
     * Genre song count
     */
    public int mSongNumber;

    /**
     * Genre album count
     */
    public int mAlbumNumber;

    /**
     * Constructor of <code>Genre</code>
     * 
     * @param genreId The Id of the genre
     * @param genreName The genre name
     */
    @Deprecated
    public Genre(final long genreId, final String genreName) {
        super();
        mGenreId = genreId;
        mGenreName = genreName;
        mSongNumber = 0;
        mAlbumNumber = 0;
    }

    /**
     * Constructor of <code>Genre</code>
     *
     * @param genreId The Id of the genre
     * @param genreName The genre name
     */
    public Genre(final long genreId, final String genreName, final int songNumber, final int albumNumber) {
        super();
        mGenreId = genreId;
        mGenreName = genreName;
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
        result = prime * result + (int) mGenreId;
        result = prime * result + (mGenreName == null ? 0 : mGenreName.hashCode());
        result = prime * result + mSongNumber;
        result = prime * result + mAlbumNumber;
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
        if (!(obj instanceof Genre)) {
            return false;
        }
        final Genre other = (Genre)obj;
        if (mGenreId != other.mGenreId) {
            return false;
        }
        if (mSongNumber != other.mSongNumber) {
            return false;
        }
        if (mAlbumNumber != other.mAlbumNumber) {
            return false;
        }
        return TextUtils.equals(mGenreName, other.mGenreName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return mGenreName;
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
        dest.writeLong(mGenreId);
        dest.writeString(mGenreName);
        dest.writeInt(mSongNumber);
        dest.writeInt(mAlbumNumber);
    }

    private Genre(Parcel in) {
        mGenreId = in.readLong();
        mGenreName = in.readString();
        mSongNumber = in.readInt();
        mAlbumNumber = in.readInt();
    }

    public static final Creator<Genre> CREATOR = new Creator<Genre>() {
        @Override
        public Genre createFromParcel(Parcel source) {
            return new Genre(source);
        }

        @Override
        public Genre[] newArray(int size) {
            return new Genre[size];
        }
    };
}
