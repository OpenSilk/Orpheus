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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class that represents a genre.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Genre implements Parcelable {

    public final long mGenreId;
    public final String mGenreName;
    public final int mSongNumber;
    public final int mAlbumNumber;
    public final long[] mSongIds;
    public final long[] mAlbumIds;

    /**
     * Constructor of <code>Genre</code>
     *
     * @param genreId The Id of the genre
     * @param genreName The genre name
     */
    public Genre(long genreId, String genreName, int songNumber, int albumNumber, long[] songIds, long[] albumIds) {
        mGenreId = genreId;
        mGenreName = genreName;
        mSongNumber = songNumber;
        mAlbumNumber = albumNumber;
        mSongIds = songIds;
        mAlbumIds = albumIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Genre)) return false;

        Genre genre = (Genre) o;

        if (mAlbumNumber != genre.mAlbumNumber) return false;
        if (mGenreId != genre.mGenreId) return false;
        if (mSongNumber != genre.mSongNumber) return false;
        if (!Arrays.equals(mAlbumIds, genre.mAlbumIds)) return false;
        if (mGenreName != null ? !mGenreName.equals(genre.mGenreName) : genre.mGenreName != null)
            return false;
        if (!Arrays.equals(mSongIds, genre.mSongIds)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (mGenreId ^ (mGenreId >>> 32));
        result = 31 * result + (mGenreName != null ? mGenreName.hashCode() : 0);
        result = 31 * result + mSongNumber;
        result = 31 * result + mAlbumNumber;
        result = 31 * result + (mSongIds != null ? Arrays.hashCode(mSongIds) : 0);
        result = 31 * result + (mAlbumIds != null ? Arrays.hashCode(mAlbumIds) : 0);
        return result;
    }

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
        dest.writeLongArray(mSongIds);
        dest.writeLongArray(mAlbumIds);
    }

    private Genre(Parcel in) {
        mGenreId = in.readLong();
        mGenreName = in.readString();
        mSongNumber = in.readInt();
        mAlbumNumber = in.readInt();
        mSongIds = in.createLongArray();
        mAlbumIds = in.createLongArray();
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
