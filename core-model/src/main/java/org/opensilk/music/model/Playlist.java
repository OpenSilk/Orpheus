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

/**
 * Created by drew on 5/2/15.
 */
public class Playlist implements Bundleable {

    public final String identity;
    public final String name;
    public final List<Uri> trackUris;
    public final List<ArtInfo> artInfos;

    protected Playlist(
            @NonNull String identity,
            @NonNull String name,
            @NonNull List<Uri> trackUris,
            @NonNull List<ArtInfo> artInfos
    ) {
        this.identity = identity;
        this.name = name;
        this.trackUris = Collections.unmodifiableList(trackUris);
        this.artInfos = Collections.unmodifiableList(artInfos);
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
        b.putString(CLZ, Playlist.class.getName());
        b.putString("_1", identity);
        b.putString("_2", name);
        b.putParcelableArrayList("_3", new ArrayList<Parcelable>(trackUris));
        b.putParcelableArrayList("_4", new ArrayList<Parcelable>(artInfos));
        return b;
    }

    protected static Playlist fromBundle(Bundle b) {
        if (!Playlist.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Playlist: "+b.getString(CLZ));
        }
        return Playlist.builder()
                .setIdentity(b.getString("_1"))
                .setName(b.getString("_2"))
                .addTrackUris(b.<Uri>getParcelableArrayList("_3"))
                .addArtInfos(b.<ArtInfo>getParcelableArrayList("_4"))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        if (identity != null ? !identity.equals(playlist.identity) : playlist.identity != null)
            return false;
        if (name != null ? !name.equals(playlist.name) : playlist.name != null) return false;
        if (trackUris != null ? !trackUris.equals(playlist.trackUris) : playlist.trackUris != null)
            return false;
        return !(artInfos != null ? !artInfos.equals(playlist.artInfos) : playlist.artInfos != null);

    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (trackUris != null ? trackUris.hashCode() : 0);
        result = 31 * result + (artInfos != null ? artInfos.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public static final BundleCreator<Playlist> BUNDLE_CREATOR = new BundleCreator<Playlist>() {
        @Override
        public Playlist fromBundle(Bundle b) throws IllegalArgumentException {
            return Playlist.fromBundle(b);
        }
    };

    public static final class Builder {
        private String identity;
        private String name;
        private ArrayList<Uri> trackUris = new ArrayList<>();
        private HashSet<ArtInfo> artInfos = new HashSet<>();

        private Builder() {
        }

        private Builder(Playlist p) {
            this.identity = p.identity;
            this.name = p.name;
            this.trackUris.addAll(p.trackUris);
            this.artInfos.addAll(p.artInfos);
        }

        public Builder setIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder addTrackUri(Uri uri) {
            this.trackUris.add(uri);
            return this;
        }

        public Builder addTrackUris(Collection<Uri> uris) {
            this.trackUris.addAll(uris);
            return this;
        }

        public Builder addArtInfo(String artist, String album, Uri uri) {
            this.artInfos.add(new ArtInfo(artist, album, uri));
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
            if (identity == null || name == null) {
                throw new NullPointerException("identity and name are required");
            }
            List<ArtInfo> artworks = Arrays.asList(artInfos.toArray(new ArtInfo[artInfos.size()]));
            Collections.sort(artworks);
            if (artworks.size() > 4) {
                artworks = artworks.subList(0, 3); //Only need 4;
            }
            return new Playlist(identity, name, trackUris, artworks);
        }
    }
}
