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
import android.os.Parcelable;
import android.support.annotation.NonNull;

import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

/**
 * Created by drew on 5/2/15.
 */
public class Playlist extends Container {

    protected Playlist(@NonNull Uri uri, @NonNull String name, @NonNull Metadata metadata) {
        super(uri, name, metadata);
    }

    public Uri getTracksUri() {
        return metadata.getUri(Metadata.KEY_CHILD_TRACKS_URI);
    }

    public int getTracksCount() {
        return metadata.getInt(Metadata.KEY_CHILD_TRACKS_COUNT);
    }

    public List<ArtInfo> getArtInfos() {
        return metadata.getArtInfos();
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, Playlist.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        return b;
    }

    protected static Playlist fromBundle(Bundle b) {
        if (!Playlist.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Playlist: "+b.getString(CLZ));
        }
        b.setClassLoader(Playlist.class.getClassLoader());
        return new Playlist(
                b.<Uri>getParcelable("_1"),
                b.getString("_2"),
                b.<Metadata>getParcelable("_3")
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final BundleCreator<Playlist> BUNDLE_CREATOR = new BundleCreator<Playlist>() {
        @Override
        public Playlist fromBundle(Bundle b) throws IllegalArgumentException {
            b.setClassLoader(getClass().getClassLoader());
            return Playlist.fromBundle(b);
        }
    };

    public static final class Builder {
        private Uri uri;
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

        public Builder setParentUri(Uri uri) {
            bob.putUri(Metadata.KEY_PARENT_URI, uri);
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

        public Playlist build() {
            if (uri == null || name == null) {
                throw new NullPointerException("uri and name are required");
            }
            bob.putArtInfos(new ArrayList<>(artInfos).subList(0, 3)); //Only need 4;
            return new Playlist(uri, name, bob.build());
        }
    }
}
