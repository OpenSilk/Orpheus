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

    protected Album(@NonNull Uri uri, @NonNull Uri parentUri, @NonNull String name, @NonNull Metadata metadata) {
        super(uri, parentUri, name, metadata);
    }

    public String getArtistName() {
        return metadata.getString(Metadata.KEY_ARTIST_NAME);
    }

    public Uri getArtistUri() {
        return metadata.getUri(Metadata.KEY_ARTIST_URI);
    }

    public int getTrackCount() {
        return metadata.getInt(Metadata.KEY_CHILD_TRACKS_COUNT);
    }

    public Uri getTracksUri() {
        return metadata.getUri(Metadata.KEY_CHILD_TRACKS_URI);
    }

    public String getYear() {
        return metadata.getString(Metadata.KEY_RELEASE_YEAR);
    }

    public Uri getArtworkUri() {
        return metadata.getUri(Metadata.KEY_ALBUM_ART_URI);
    }

    public Uri getDetailsUri() {
        return metadata.getUri(Metadata.KEY_DETAILS_URI);
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, Album.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        b.putParcelable("_4", parentUri);
        return b;
    }

    protected static Album fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Album.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Album: "+b.getString(CLZ));
        }
        b.setClassLoader(Album.class.getClassLoader());
        return new Album(
                b.<Uri>getParcelable("_1"),
                b.<Uri>getParcelable("_4"),
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

        public Builder setArtistName(String artistName) {
            bob.putString(Metadata.KEY_ARTIST_NAME, artistName);
            return this;
        }

        public Builder setArtistUri(Uri artistUri) {
            bob.putUri(Metadata.KEY_ARTIST_URI, artistUri);
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

        public Builder setYear(String year) {
            bob.putString(Metadata.KEY_RELEASE_YEAR, year);
            return this;
        }

        public Builder setArtworkUri(Uri artworkUri) {
            bob.putUri(Metadata.KEY_ALBUM_ART_URI, artworkUri);
            return this;
        }

        public Builder setDetailsUri(Uri detailsUri) {
            bob.putUri(Metadata.KEY_DETAILS_URI, detailsUri);
            return this;
        }

        public Album build() {
            if (uri == null || parentUri == null || name == null) {
                throw new NullPointerException("uri and name are required");
            }
            return new Album(uri, parentUri, name, bob.build());
        }
    }

}
