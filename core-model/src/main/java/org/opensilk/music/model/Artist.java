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
public class Artist extends Container {

    protected Artist(@NonNull Uri uri, @NonNull String name, @NonNull Metadata metadata) {
        super(uri, name, metadata);
    }

    public int getAlbumCount() {
        return metadata.getInt(Metadata.KEY_CHILD_ALBUMS_COUNT);
    }

    public int getTrackCount() {
        return metadata.getInt(Metadata.KEY_CHILD_TRACKS_COUNT);
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, Artist.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        return b;
    }

    protected static Artist fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Artist.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Artist: "+b.getString(CLZ));
        }
        b.setClassLoader(Artist.class.getClassLoader());
        return new Artist(
                b.<Uri>getParcelable("_1"),
                b.getString("_2"),
                b.<Metadata>getParcelable("_3")
        );
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

        public Builder setAlbumCount(int albumCount) {
            bob.putInt(Metadata.KEY_CHILD_ALBUMS_COUNT, albumCount);
            return this;
        }

        public Builder setTrackCount(int trackCount) {
            bob.putInt(Metadata.KEY_CHILD_TRACKS_COUNT, trackCount);
            return this;
        }

        public Artist build() {
            if (uri == null || name == null) {
                throw new NullPointerException("uri and name are required");
            }
            return new Artist(uri, name, bob.build());
        }
    }
}
