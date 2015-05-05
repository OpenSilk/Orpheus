/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.model;

import android.os.Bundle;
import android.support.annotation.NonNull;

import org.opensilk.music.model.spi.Bundleable;

/**
 * Created by drew on 6/10/14.
 */
public class Artist implements Bundleable {

    public final String identity;
    public final String name;
    public final int albumCount;
    public final int trackCount;

    protected Artist(@NonNull String identity,
                     @NonNull String name,
                     int albumCount,
                     int trackCount
    ) {
        this.identity = identity;
        this.name = name;
        this.albumCount = albumCount;
        this.trackCount = trackCount;
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(10); //2x
        b.putString(CLZ, Artist.class.getName());
        b.putString("_1", identity);
        b.putString("_2", name);
        b.putInt("_3", albumCount);
        b.putInt("_4", trackCount);
        return b;
    }

    protected static Artist fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Artist.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Artist: "+b.getString(CLZ));
        }
        return new Builder()
                .setIdentity(b.getString("_1"))
                .setName(b.getString("_2"))
                .setAlbumCount(b.getInt("_3"))
                .setTrackCount(b.getInt("_4"))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist)) return false;

        Artist artist = (Artist) o;

        if (albumCount != artist.albumCount) return false;
        if (trackCount != artist.trackCount) return false;
        if (identity != null ? !identity.equals(artist.identity) : artist.identity != null)
            return false;
        if (name != null ? !name.equals(artist.name) : artist.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + albumCount;
        result = 31 * result + trackCount;
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final BundleCreator<Artist> BUNDLE_CREATOR = new BundleCreator<Artist>() {
        @Override
        public Artist fromBundle(Bundle b) throws IllegalArgumentException {
            return Artist.fromBundle(b);
        }
    };

    public static final class Builder {
        private String identity;
        private String name;
        private int albumCount;
        private int trackCount;

        public Builder setIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setAlbumCount(int albumCount) {
            this.albumCount = albumCount;
            return this;
        }

        public Builder setTrackCount(int trackCount) {
            this.trackCount = trackCount;
            return this;
        }

        public Artist build() {
            if (identity == null || name == null) {
                throw new NullPointerException("identity and name are required");
            }
            return new Artist(identity, name, albumCount, trackCount);
        }
    }
}
