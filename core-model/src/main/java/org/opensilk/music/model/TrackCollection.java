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
public abstract class TrackCollection implements Bundleable {

    public final String identity;
    public final String name;
    public final List<Uri> trackUris;
    public final List<Uri> albumUris;
    public final List<ArtInfo> artInfos;

    protected TrackCollection(
            @NonNull String identity,
            @NonNull String name,
            @NonNull List<Uri> trackUris,
            @NonNull List<Uri> albumUris,
            @NonNull List<ArtInfo> artInfos
    ) {
        this.identity = identity;
        this.name = name;
        this.trackUris = Collections.unmodifiableList(trackUris);
        this.albumUris = Collections.unmodifiableList(albumUris);
        this.artInfos = Collections.unmodifiableList(artInfos);
    }

    protected TrackCollection(Bundle b) {
        this(
                b.getString("_1"),
                b.getString("_2"),
                Arrays.asList((Uri[])b.getParcelableArray("_3")),
                Arrays.asList((Uri[])b.getParcelableArray("_4")),
                Arrays.asList((ArtInfo[])b.getParcelableArray("_5"))
        );
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(10); //2x
        b.putString("clz", getClz());
        b.putString("_1", identity);
        b.putString("_2", name);
        b.putParcelableArray("_3", trackUris.toArray(new Uri[trackUris.size()]));
        b.putParcelableArray("_4", albumUris.toArray(new Uri[albumUris.size()]));
        b.putParcelableArray("_5", artInfos.toArray(new ArtInfo[artInfos.size()]));
        return null;
    }

    protected abstract String getClz();

    @Override
    public String getIdentity() {
        return identity;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackCollection that = (TrackCollection) o;
        if (identity != null ? !identity.equals(that.identity) : that.identity != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (trackUris != null ? !trackUris.equals(that.trackUris) : that.trackUris != null)
            return false;
        if (albumUris != null ? !albumUris.equals(that.albumUris) : that.albumUris != null)
            return false;
        return !(artInfos != null ? !artInfos.equals(that.artInfos) : that.artInfos != null);

    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (trackUris != null ? trackUris.hashCode() : 0);
        result = 31 * result + (albumUris != null ? albumUris.hashCode() : 0);
        result = 31 * result + (artInfos != null ? artInfos.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static abstract class Builder<T extends TrackCollection> {
        protected String identity;
        protected String name;
        protected ArrayList<Uri> trackUris = new ArrayList<>();
        protected HashSet<Uri> albumUris = new HashSet<>();
        protected HashSet<ArtInfo> artInfos = new HashSet<>();

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

        public Builder addAlbumUri(Uri uri) {
            this.albumUris.add(uri);
            return this;
        }

        public Builder addAlbumUris(Collection<Uri> uris) {
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

        public Builder addArtworkInfos(Collection<ArtInfo> infos) {
            this.artInfos.addAll(infos);
            return this;
        }

        public abstract T build();
    }

}
