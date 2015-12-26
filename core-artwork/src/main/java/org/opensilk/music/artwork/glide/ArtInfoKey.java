/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.artwork.glide;

import com.bumptech.glide.load.Key;

import org.opensilk.music.model.ArtInfo;

import java.security.MessageDigest;

/**
 * Created by drew on 12/25/15.
 */
public class ArtInfoKey implements Key {
    final ArtInfo artInfo;

    public ArtInfoKey(ArtInfo artInfo) {
        this.artInfo = artInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArtInfoKey that = (ArtInfoKey) o;
        return artInfo.equals(that.artInfo);
    }

    @Override
    public int hashCode() {
        return artInfo.hashCode();
    }

    @Override
    public String toString() {
        return artInfo.toString();
    }

    @Override
    public void updateDiskCacheKey(MessageDigest messageDigest) {
        messageDigest.update(artInfo.cacheKey().getBytes(CHARSET));
    }
}
