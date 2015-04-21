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

import com.andrew.apollo.utils.Lists;

import java.util.Arrays;
import java.util.List;

/**
 * A class that represents a playlist.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Playlist implements Parcelable {

    public final long mPlaylistId;
    public final String mPlaylistName;
    public final int mSongNumber;
    public final int mAlbumNumber;
    public final long[] mSongIds;
    public final long[] mAlbumIds;

    public Playlist(long mPlaylistId, String mPlaylistName, int mSongNumber,
                    int mAlbumNumber, long[] mSongIds, long[] mAlbumIds) {
        this.mPlaylistId = mPlaylistId;
        this.mPlaylistName = mPlaylistName;
        this.mSongNumber = mSongNumber;
        this.mAlbumNumber = mAlbumNumber;
        this.mSongIds = mSongIds;
        this.mAlbumIds = mAlbumIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Playlist)) return false;

        Playlist playlist = (Playlist) o;

        if (mAlbumNumber != playlist.mAlbumNumber) return false;
        if (mPlaylistId != playlist.mPlaylistId) return false;
        if (mSongNumber != playlist.mSongNumber) return false;
        if (!Arrays.equals(mAlbumIds, playlist.mAlbumIds)) return false;
        if (mPlaylistName != null ? !mPlaylistName.equals(playlist.mPlaylistName) : playlist.mPlaylistName != null)
            return false;
        if (!Arrays.equals(mSongIds, playlist.mSongIds)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (mPlaylistId ^ (mPlaylistId >>> 32));
        result = 31 * result + (mPlaylistName != null ? mPlaylistName.hashCode() : 0);
        result = 31 * result + mSongNumber;
        result = 31 * result + mAlbumNumber;
        result = 31 * result + (mSongIds != null ? Arrays.hashCode(mSongIds) : 0);
        result = 31 * result + (mAlbumIds != null ? Arrays.hashCode(mAlbumIds) : 0);
        return result;
    }

    @Override
    public String toString() {
        return mPlaylistName;
    }

    /*
     * Implement the Parcelable interface
     */

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mPlaylistId);
        dest.writeString(mPlaylistName);
        dest.writeInt(mSongNumber);
        dest.writeInt(mAlbumNumber);
        dest.writeLongArray(mSongIds);
        dest.writeLongArray(mAlbumIds);
    }

    private Playlist(Parcel in) {
        mPlaylistId = in.readLong();
        mPlaylistName = in.readString();
        mSongNumber = in.readInt();
        mAlbumNumber = in.readInt();
        mSongIds = in.createLongArray();
        mAlbumIds = in.createLongArray();
    }

    public static final Creator<Playlist> CREATOR = new Creator<Playlist>() {
        @Override
        public Playlist createFromParcel(Parcel source) {
            return new Playlist(source);
        }

        @Override
        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

}
