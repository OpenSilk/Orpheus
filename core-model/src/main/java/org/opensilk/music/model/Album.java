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

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.opensilk.music.model.spi.Bundleable;

/**
 * Created by drew on 6/10/14.
 */
public class Album implements Bundleable {

    public final String identity;
    public final String name;
    public final String artistName;
    public final int trackCount;
    public final String date;
    public final Uri artworkUri;

    protected Album(@NonNull String identity,
                    @NonNull String name,
                    @Nullable String artistName,
                    int trackCount,
                    @Nullable String date,
                    @Nullable Uri artworkUri
    ) {
        this.identity = identity;
        this.name = name;
        this.artistName = artistName;
        this.trackCount = trackCount;
        this.date = date;
        this.artworkUri = artworkUri;
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
        Bundle b = new Bundle(14); //2x
        b.putString("clz", Album.class.getName());
        b.putString("_1", identity);
        b.putString("_2", name);
        b.putString("_3", artistName);
        b.putInt("_4", trackCount);
        b.putString("_5", date);
        b.putParcelable("_6", artworkUri);
        return b;
    }

    protected static Album fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Album.class.getName().equals(b.getString("clz"))) {
            throw new IllegalArgumentException("Wrong class for Album: "+b.getString("clz"));
        }
        return new Builder()
                .setIdentity(b.getString("_1"))
                .setName(b.getString("_2"))
                .setArtistName(b.getString("_3"))
                .setTrackCount(b.getInt("_4"))
                .setDate(b.getString("_5"))
                .setArtworkUri(b.<Uri>getParcelable("_6"))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Album)) return false;

        Album album = (Album) o;

        if (trackCount != album.trackCount) return false;
        if (artistName != null ? !artistName.equals(album.artistName) : album.artistName != null)
            return false;
        if (artworkUri != null ? !artworkUri.equals(album.artworkUri) : album.artworkUri != null)
            return false;
        if (date != null ? !date.equals(album.date) : album.date != null) return false;
        if (identity != null ? !identity.equals(album.identity) : album.identity != null)
            return false;
        if (name != null ? !name.equals(album.name) : album.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (artistName != null ? artistName.hashCode() : 0);
        result = 31 * result + trackCount;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + (artworkUri != null ? artworkUri.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final BundleCreator<Album> BUNDLE_CREATOR = new BundleCreator<Album>() {
        @Override
        public Album fromBundle(Bundle b) throws IllegalArgumentException {
            return Album.fromBundle(b);
        }
    };

    public static final class Builder {
        private String identity;
        private String name;
        private String artistName;
        private int trackCount;
        private String date;

        private Uri artworkUri;

        public Builder setIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setArtistName(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder setTrackCount(int trackCount) {
            this.trackCount = trackCount;
            return this;
        }

        public Builder setDate(String date) {
            this.date = date;
            return this;
        }

        public Builder setArtworkUri(Uri artworkUri) {
            this.artworkUri = artworkUri;
            return this;
        }

        public Album build() {
            if (identity == null || name == null) {
                throw new NullPointerException("identity and name are required");
            }
            return new Album(identity, name, artistName, trackCount, date, artworkUri);
        }
    }

}
