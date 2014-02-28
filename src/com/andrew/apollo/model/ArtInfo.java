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

import android.text.TextUtils;

/**
 * All the info we need to fetch album art
 *
 * Created by drew on 2/26/14.
 */
public class ArtInfo {

    /**
     * The song artist
     */
    public final String mArtistName;

    /**
     * The song album
     */
    public final String mAlbumName;

    /**
     * Album Id
     */
    public final long mAlbumId;

    public ArtInfo(String artistName, String albumName, long albumId) {
        mArtistName = artistName;
        mAlbumName = albumName;
        mAlbumId = albumId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (mArtistName == null ? 0 : mArtistName.hashCode());
        result = prime * result + (mAlbumName == null ? 0 : mAlbumName.hashCode());
        result = prime * result + (int) mAlbumId;
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Song other = (Song)obj;
        if (!TextUtils.equals(mArtistName, other.mArtistName)) {
            return false;
        }
        if (!TextUtils.equals(mAlbumName, other.mAlbumName)) {
            return false;
        }
        if (mAlbumId != other.mAlbumId) {
            return false;
        }
        return true;
    }

}
