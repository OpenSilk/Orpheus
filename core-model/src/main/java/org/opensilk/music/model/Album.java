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

/**
 * Created by drew on 6/10/14.
 */
public class Album extends Container {

    protected Album(@NonNull Uri uri, @NonNull String name, @NonNull Metadata metadata) {
        super(uri, name, metadata);
    }

    public String getArtistName() {
        return metadata.getString(Metadata.KEY_ARTIST_NAME);
    }

    public int getTrackCount() {
        return metadata.getInt(Metadata.KEY_CHILD_TRACKS_COUNT);
    }

    public String getYear() {
        return metadata.getString(Metadata.KEY_YEAR);
    }

    public Uri getArtworkUri() {
        return metadata.getUri(Metadata.KEY_ALBUM_ART_URI);
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, Album.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        return b;
    }

    protected static Album fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Album.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Album: "+b.getString(CLZ));
        }
        b.setClassLoader(Album.class.getClassLoader());
        return new Album(
                b.<Uri>getParcelable("_1"),
                b.getString("_2"),
                b.<Metadata>getParcelable("_3")
        );
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
        private Uri uri;
        private String name;
        private Metadata.Builder bob = Metadata.builder();

        private Builder() {
        }

        public Builder setUri(Uri uri) {
            this.uri = uri;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setParentUri(Uri uri) {
            bob.putUri(Metadata.KEY_PARENT_URI, uri);
            return this;
        }

        public Builder setArtistName(String artistName) {
            bob.putString(Metadata.KEY_ARTIST_NAME, artistName);
            return this;
        }

        public Builder setTrackCount(int trackCount) {
            bob.putInt(Metadata.KEY_CHILD_TRACKS_COUNT, trackCount);
            return this;
        }

        public Builder setYear(String year) {
            bob.putString(Metadata.KEY_YEAR, year);
            return this;
        }

        public Builder setArtworkUri(Uri artworkUri) {
            bob.putUri(Metadata.KEY_ALBUM_ART_URI, artworkUri);
            return this;
        }

        public Album build() {
            if (uri == null || name == null) {
                throw new NullPointerException("uri and name are required");
            }
            return new Album(uri, name, bob.build());
        }
    }

}
