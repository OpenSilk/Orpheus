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
import java.util.List;

/**
 * Created by drew on 5/2/15.
 */
public abstract class TrackCollection implements Bundleable {

    public final String identity;
    public final String name;
    public final List<Uri> trackUris;
    public final List<Uri> albumUris;
    public final List<Uri> artworkUris;

    protected TrackCollection(
            @NonNull String identity,
            @NonNull String name,
            @NonNull List<Uri> trackUris,
            @NonNull List<Uri> albumUris,
            @NonNull List<Uri> artworkUris
    ) {
        this.identity = identity;
        this.name = name;
        this.trackUris = trackUris;
        this.albumUris = albumUris;
        this.artworkUris = artworkUris;
    }

    protected TrackCollection(Bundle b) {
        if (!getClz().equals(b.getString("clz"))) {
            throw new IllegalArgumentException("Wrong class for TrackCollection: "+b.getString("clz"));
        }
        this.identity = b.getString("_1");
        this.name = b.getString("_2");
        this.trackUris = new ArrayList<>(Arrays.asList((Uri[])b.getParcelableArray("_3")));
        this.albumUris = new ArrayList<>(Arrays.asList((Uri[])b.getParcelableArray("_4")));
        this.artworkUris = new ArrayList<>(Arrays.asList((Uri[])b.getParcelableArray("_5")));
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(10); //2x
        b.putString("clz", getClz());
        b.putString("_1", identity);
        b.putString("_2", name);
        b.putParcelableArray("_3", trackUris.toArray(new Uri[trackUris.size()]));
        b.putParcelableArray("_4", albumUris.toArray(new Uri[albumUris.size()]));
        b.putParcelableArray("_5", artworkUris.toArray(new Uri[artworkUris.size()]));
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
        return !(artworkUris != null ? !artworkUris.equals(that.artworkUris) : that.artworkUris != null);

    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (trackUris != null ? trackUris.hashCode() : 0);
        result = 31 * result + (albumUris != null ? albumUris.hashCode() : 0);
        result = 31 * result + (artworkUris != null ? artworkUris.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static abstract class Builder<T extends TrackCollection> {
        protected String identity;
        protected String name;
        protected List<Uri> trackUris = new ArrayList<>();
        protected List<Uri> albumUris = new ArrayList<>();
        protected List<Uri> artworkUris = new ArrayList<>();

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

        public Builder addArtworkUri(Uri uri) {
            this.artworkUris.add(uri);
            return this;
        }

        public Builder addArtworkUris(Collection<Uri> uris) {
            this.artworkUris.addAll(uris);
            return this;
        }

        public abstract T build();
    }

}
