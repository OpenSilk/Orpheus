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

import java.util.List;

/**
 * A class that represents a playlist.
 * 
 * @author Andrew Neal (andrewdneal@gmail.com)
 */
public class Playlist implements Parcelable {

    /**
     * The unique Id of the playlist
     */
    public long mPlaylistId;

    /**
     * The playlist name
     */
    public String mPlaylistName;

    /**
     * Songs in playlist
     */
    public List<Song> mSongs;

    /**
     * Constructor of <code>Playlist</code>
     * 
     * @param playlistId The Id of the playlist
     * @param playlistName The playlist name
     */
    @Deprecated
    public Playlist(final long playlistId, final String playlistName) {
        super();
        mPlaylistId = playlistId;
        mPlaylistName = playlistName;
        mSongs = Lists.newArrayList();
    }

    /**
     * Constructor of <code>Playlist</code>
     *
     * @param playlistId The Id of the playlist
     * @param playlistName The playlist name
     */
    public Playlist(final long playlistId, final String playlistName, final List<Song> songs) {
        super();
        mPlaylistId = playlistId;
        mPlaylistName = playlistName;
        mSongs = songs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) mPlaylistId;
        result = prime * result + (mPlaylistName == null ? 0 : mPlaylistName.hashCode());
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
        if (!(obj instanceof Playlist)) {
            return false;
        }
        final Playlist other = (Playlist)obj;
        if (mPlaylistId != other.mPlaylistId) {
            return false;
        }
        return TextUtils.equals(mPlaylistName, other.mPlaylistName);
    }

    /**
     * {@inheritDoc}
     */
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
    }

    private Playlist(Parcel in) {
        mPlaylistId = in.readLong();
        mPlaylistName = in.readString();
        mSongs = null;
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
