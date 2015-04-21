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

package org.opensilk.music.api.model;

import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.opensilk.music.api.model.spi.Bundleable;

/**
 * Created by drew on 6/10/14.
 */
public class Song implements Bundleable {

    public static final String DEFAULT_MIME_TYPE = "audio/*";

    public final String identity;
    public final String name;
    public final String albumName;
    public final String artistName;
    public final String albumArtistName;
    public final String albumIdentity;
    public final int duration;
    public final Uri dataUri;
    public final Uri artworkUri;
    public final String mimeType;

    protected Song(@NonNull String identity,
                @NonNull String name,
                @Nullable String albumName,
                @Nullable String artistName,
                @Nullable String albumArtistName,
                @Nullable String albumIdentity,
                int duration,
                @NonNull Uri dataUri,
                @Nullable Uri artworkUri,
                @Nullable String mimeType) {
        this.identity = identity;
        this.name = name;
        this.albumName = albumName;
        this.artistName = artistName;
        this.albumArtistName = albumArtistName;
        this.albumIdentity = albumIdentity;
        this.duration = duration;
        this.dataUri = dataUri;
        this.artworkUri = artworkUri;
        this.mimeType = mimeType != null ? mimeType : DEFAULT_MIME_TYPE;
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
        Bundle b = new Bundle(22); //2x
        b.putString("clz", Song.class.getName());
        b.putString("_1", identity);
        b.putString("_2", name);
        b.putString("_3", albumName);
        b.putString("_4", artistName);
        b.putString("_5", albumArtistName);
        b.putString("_6", albumIdentity);
        b.putInt("_7", duration);
        b.putParcelable("_8", dataUri);
        b.putParcelable("_9", artworkUri);
        b.putString("_10", mimeType);
        return b;
    }

    protected static Song fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Song.class.getName().equals(b.getString("clz"))) {
            throw new IllegalArgumentException("Wrong class for Song: "+b.getString("clz"));
        }
        return new Builder()
                .setIdentity(b.getString("_1"))
                .setName(b.getString("_2"))
                .setAlbumName(b.getString("_3"))
                .setArtistName(b.getString("_4"))
                .setAlbumArtistName(b.getString("_5"))
                .setAlbumIdentity(b.getString("_6"))
                .setDuration(b.getInt("_7"))
                .setDataUri((Uri) b.getParcelable("_8"))
                .setArtworkUri((Uri) b.getParcelable("_9"))
                .setMimeType(b.getString("_10"))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Song)) return false;

        Song song = (Song) o;

        if (duration != song.duration) return false;
        if (albumArtistName != null ? !albumArtistName.equals(song.albumArtistName) : song.albumArtistName != null)
            return false;
        if (albumIdentity != null ? !albumIdentity.equals(song.albumIdentity) : song.albumIdentity != null)
            return false;
        if (albumName != null ? !albumName.equals(song.albumName) : song.albumName != null)
            return false;
        if (artistName != null ? !artistName.equals(song.artistName) : song.artistName != null)
            return false;
        if (artworkUri != null ? !artworkUri.equals(song.artworkUri) : song.artworkUri != null)
            return false;
        if (dataUri != null ?  !dataUri.equals(song.dataUri) : song.dataUri != null) return false;
        if (identity != null ? !identity.equals(song.identity) : song.identity != null)
            return false;
        if (name != null ? !name.equals(song.name) : song.name != null) return false;
        if (mimeType != null ? !mimeType.equals(song.mimeType) : song.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (albumName != null ? albumName.hashCode() : 0);
        result = 31 * result + (artistName != null ? artistName.hashCode() : 0);
        result = 31 * result + (albumArtistName != null ? albumArtistName.hashCode() : 0);
        result = 31 * result + (albumIdentity != null ? albumIdentity.hashCode() : 0);
        result = 31 * result + duration;
        result = 31 * result + (dataUri != null ? dataUri.hashCode() : 0);
        result = 31 * result + (artworkUri != null ? artworkUri.hashCode() : 0);
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final BundleCreator<Song> BUNDLE_CREATOR = new BundleCreator<Song>() {
        @Override
        public Song fromBundle(Bundle b) throws IllegalArgumentException {
            return Song.fromBundle(b);
        }
    };

    public static final class Builder {
        private String identity;
        private String name;
        private String albumName;
        private String artistName;
        private String albumArtistName;
        private String albumIdentity;
        private int duration;
        private Uri dataUri;
        private Uri artworkUri;
        private String mimeType = DEFAULT_MIME_TYPE;

        public Builder setIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setAlbumName(String albumName) {
            this.albumName = albumName;
            return this;
        }

        public Builder setArtistName(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder setAlbumArtistName(String albumArtistName) {
            this.albumArtistName = albumArtistName;
            return this;
        }

        public Builder setAlbumIdentity(String albumIdentity) {
            this.albumIdentity = albumIdentity;
            return this;
        }

        public Builder setDuration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder setDataUri(Uri dataUri) {
            this.dataUri = dataUri;
            return this;
        }

        public Builder setArtworkUri(Uri artworkUri) {
            this.artworkUri = artworkUri;
            return this;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public Song build() {
            if (identity == null || name == null || dataUri == null) {
                throw new NullPointerException("identity, name, and dataUri are required");
            }
            return new Song(identity, name, albumName, artistName, albumArtistName,
                    albumIdentity, duration, dataUri, artworkUri, mimeType);
        }
    }
}
