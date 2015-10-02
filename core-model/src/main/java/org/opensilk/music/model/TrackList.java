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

package org.opensilk.music.model;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;

import org.opensilk.bundleable.Bundleable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by drew on 5/12/15.
 */
public class TrackList extends Container {

    protected TrackList(@NonNull Uri uri, @NonNull Uri parentUri,
                        @NonNull String name, @NonNull Metadata metadata) {
        super(uri, parentUri, name, metadata);
    }

    public Uri getTracksUri() {
        return metadata.getUri(Metadata.KEY_CHILD_TRACKS_URI);
    }

    public int getTracksCount() {
        return metadata.getInt(Metadata.KEY_CHILD_TRACKS_COUNT);
    }

    public Uri getAlbumsUri() {
        return metadata.getUri(Metadata.KEY_CHILD_ALBUMS_URI);
    }

    public int getAlbumsCount() {
        return metadata.getInt(Metadata.KEY_CHILD_ALBUMS_COUNT);
    }

    public List<ArtInfo> getArtInfos() {
        return metadata.getArtInfos();
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, TrackList.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        b.putParcelable("_4", parentUri);
        return b;
    }

    protected static TrackList fromBundle(Bundle b) {
        if (!TrackList.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for TrackCollection: "+b.getString(CLZ));
        }
        b.setClassLoader(TrackList.class.getClassLoader());
        return new TrackList(
                b.<Uri>getParcelable("_1"),
                b.<Uri>getParcelable("_4"),
                b.getString("_2"),
                b.<Metadata>getParcelable("_3")
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final Bundleable.BundleCreator<TrackList> BUNDLE_CREATOR = new Bundleable.BundleCreator<TrackList>() {
        @Override
        public TrackList fromBundle(Bundle b) throws IllegalArgumentException {
            b.setClassLoader(getClass().getClassLoader());
            return TrackList.fromBundle(b);
        }
    };

    public static final class Builder  {
        private Uri uri;
        private Uri parentUri;
        private String name;
        private Metadata.Builder bob = Metadata.builder();
        private TreeSet<ArtInfo> artInfos = new TreeSet<>();

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

        public Builder setTracksUri(Uri uri) {
            bob.putUri(Metadata.KEY_CHILD_TRACKS_URI, uri);
            return this;
        }

        public Builder setTrackCount(int count) {
            bob.putInt(Metadata.KEY_CHILD_TRACKS_COUNT, count);
            return this;
        }

        public Builder setAlbumsUri(Uri uri) {
            bob.putUri(Metadata.KEY_CHILD_ALBUMS_URI, uri);
            return this;
        }

        public Builder setAlbumCount(int count) {
            bob.putInt(Metadata.KEY_CHILD_TRACKS_COUNT, count);
            return this;
        }

        public Builder addArtInfo(String artist, String album, Uri uri) {
            this.artInfos.add(ArtInfo.forAlbum(artist, album, uri));
            return this;
        }

        public Builder addArtInfo(ArtInfo info) {
            this.artInfos.add(info);
            return this;
        }

        public Builder addArtInfos(Collection<ArtInfo> infos) {
            this.artInfos.addAll(infos);
            return this;
        }

        public TrackList build() {
            if (uri == null || parentUri == null || name == null) {
                throw new NullPointerException("uri and name are required");
            }
            bob.putArtInfos(artInfos.size() > 4 ? new ArrayList<>(artInfos).subList(0, 3) : artInfos); //Only need 4;
            return new TrackList(uri, parentUri, name, bob.build());
        }
    }
}
