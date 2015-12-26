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

    protected Artist(@NonNull Uri uri, @NonNull Uri parentUri,
                     @NonNull String name, @NonNull Metadata metadata) {
        super(uri, parentUri, name, metadata);
    }

    public int getAlbumCount() {
        return metadata.getInt(Metadata.KEY_CHILD_ALBUMS_COUNT);
    }

    public int getTrackCount() {
        return metadata.getInt(Metadata.KEY_CHILD_TRACKS_COUNT);
    }

    public Uri getTracksUri() {
        return metadata.getUri(Metadata.KEY_CHILD_TRACKS_URI);
    }

    public Uri getDetailsUri() {
        return metadata.getUri(Metadata.KEY_DETAILS_URI);
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, Artist.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        b.putParcelable("_4", parentUri);
        return b;
    }

    protected static Artist fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Artist.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Artist: "+b.getString(CLZ));
        }
        b.setClassLoader(Artist.class.getClassLoader());
        return new Artist(
                b.<Uri>getParcelable("_1"),
                b.<Uri>getParcelable("_4"),
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
        private Uri parentUri;
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

        public Builder setSortName(String name) {
            bob.putString(Metadata.KEY_SORT_NAME, name);
            return this;
        }

        public Builder setParentUri(Uri uri) {
            this.parentUri = uri;
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

        public Builder setTracksUri(Uri uri) {
            bob.putUri(Metadata.KEY_CHILD_TRACKS_URI, uri);
            return this;
        }

        public Builder setDetailsUri(Uri detailsUri) {
            bob.putUri(Metadata.KEY_DETAILS_URI, detailsUri);
            return this;
        }

        public Artist build() {
            if (uri == null || parentUri == null || name == null) {
                throw new NullPointerException("uri and name are required");
            }
            return new Artist(uri, parentUri, name, bob.build());
        }
    }
}
