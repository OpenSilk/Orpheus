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
 * Created by drew on 5/12/15.
 */
public class TrackCollection implements Bundleable {

    public final String name;
    public final Uri tracksUri;
    public final int trackCount;
    public final int albumCount;
    public final List<ArtInfo> artInfos;

    protected TrackCollection(
            @NonNull String name,
            @NonNull Uri tracksUri,
            int trackCount,
            int albumCount,
            @NonNull List<ArtInfo> artInfos
    ) {
        this.name = name;
        this.tracksUri = tracksUri;
        this.trackCount = trackCount;
        this.albumCount = albumCount;
        this.artInfos = Collections.unmodifiableList(artInfos);
    }

    @Override
    public String getIdentity() {
        return tracksUri.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(10); //2x
        b.putString(CLZ, TrackCollection.class.getName());
        b.putString("_1", name);
        b.putParcelable("_2", tracksUri);
        b.putInt("_3", trackCount);
        b.putInt("_4", albumCount);
        b.putParcelableArrayList("_5", new ArrayList<Parcelable>(artInfos));
        return b;
    }

    protected static TrackCollection fromBundle(Bundle b) {
        if (!TrackCollection.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for TrackCollection: "+b.getString(CLZ));
        }
        return TrackCollection.builder()
                .setName(b.getString("_1"))
                .setTracksUri(b.<Uri>getParcelable("_2"))
                .setTrackCount(b.getInt("_3"))
                .setAlbumCount(b.getInt("_4"))
                .addArtInfos(b.<ArtInfo>getParcelableArrayList("_5"))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackCollection that = (TrackCollection) o;
        if (trackCount != that.trackCount) return false;
        if (albumCount != that.albumCount) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (tracksUri != null ? !tracksUri.equals(that.tracksUri) : that.tracksUri != null)
            return false;
        return !(artInfos != null ? !artInfos.equals(that.artInfos) : that.artInfos != null);

    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (tracksUri != null ? tracksUri.hashCode() : 0);
        result = 31 * result + trackCount;
        result = 31 * result + albumCount;
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

    public static final Bundleable.BundleCreator<TrackCollection> BUNDLE_CREATOR = new Bundleable.BundleCreator<TrackCollection>() {
        @Override
        public TrackCollection fromBundle(Bundle b) throws IllegalArgumentException {
            b.setClassLoader(getClass().getClassLoader());
            return TrackCollection.fromBundle(b);
        }
    };

    public static final class Builder  {
        private String name;
        private Uri tracksUri;
        private int trackCount;
        private int albumCount;
        private HashSet<ArtInfo> artInfos = new HashSet<>();

        private Builder() {
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setTracksUri(Uri tracksUri) {
            this.tracksUri = tracksUri;
            return this;
        }

        public Builder setTrackCount(int trackCount) {
            this.trackCount = trackCount;
            return this;
        }

        public Builder setAlbumCount(int albumCount) {
            this.albumCount = albumCount;
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

        public TrackCollection build() {
            if (name == null || tracksUri == null) {
                throw new NullPointerException("name and tracksUri are required");
            }
            List<ArtInfo> artworks = Arrays.asList(artInfos.toArray(new ArtInfo[artInfos.size()]));
            Collections.sort(artworks);
            if (artworks.size() > 4) {
                artworks = artworks.subList(0, 3); //Only need 4;
            }
            return new TrackCollection(name, tracksUri, trackCount, albumCount, artworks);
        }
    }
}
