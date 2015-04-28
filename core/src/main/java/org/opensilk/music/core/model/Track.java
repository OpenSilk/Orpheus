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

package org.opensilk.music.core.model;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.opensilk.music.core.spi.Bundleable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by drew on 6/10/14.
 */
public class Track implements Bundleable {

    public static final String DEFAULT_MIME_TYPE = "audio/*";

    public final String identity;
    public final String name;
    public final String albumName;
    public final String artistName;
    public final String albumArtistName;
    public final String albumIdentity;
    public final int duration;
    public final Uri dataUri;
    public final Uri artworkUri;
    public final String mimeType;
    private final String headers;

    protected Track(@NonNull String identity,
                    @NonNull String name,
                    @Nullable String albumName,
                    @Nullable String artistName,
                    @Nullable String albumArtistName,
                    @Nullable String albumIdentity,
                    int duration,
                    @NonNull Uri dataUri,
                    @Nullable Uri artworkUri,
                    @Nullable String mimeType,
                    @Nullable String headers
    ) {
        this.identity = identity;
        this.name = name;
        this.albumName = albumName;
        this.artistName = artistName;
        this.albumArtistName = albumArtistName;
        this.albumIdentity = albumIdentity;
        this.duration = duration;
        this.dataUri = dataUri;
        this.artworkUri = artworkUri;
        this.mimeType = mimeType != null ? mimeType : DEFAULT_MIME_TYPE;
        this.headers = headers;
    }

    @Override
    public String getIdentity() {
        return identity;
    }

    @Override
    public String getName() {
        return name;
    }

    @NonNull
    public Map<String, String> getHeaders() {
        if (TextUtils.isEmpty(headers)) {
            return Collections.emptyMap();
        }
        HashMap<String, String> hdrs = new HashMap<>();
        String[] lines = headers.split("\n");
        for (String line : lines) {
            String[] entry = line.split(":");
            if (entry.length == 2) {
                hdrs.put(entry[0].trim(), entry[1].trim());
            }
        }
        return hdrs;
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(22); //2x
        b.putString("clz", Track.class.getName());
        b.putString("_1", identity);
        b.putString("_2", name);
        b.putString("_3", albumName);
        b.putString("_4", artistName);
        b.putString("_5", albumArtistName);
        b.putString("_6", albumIdentity);
        b.putInt("_7", duration);
        b.putParcelable("_8", dataUri);
        b.putParcelable("_9", artworkUri);
        b.putString("_10", mimeType);
        b.putString("_11", headers);
        return b;
    }

    protected static Track fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Track.class.getName().equals(b.getString("clz"))) {
            throw new IllegalArgumentException("Wrong class for Song: "+b.getString("clz"));
        }
        return new Builder()
                .setIdentity(b.getString("_1"))
                .setName(b.getString("_2"))
                .setAlbumName(b.getString("_3"))
                .setArtistName(b.getString("_4"))
                .setAlbumArtistName(b.getString("_5"))
                .setAlbumIdentity(b.getString("_6"))
                .setDuration(b.getInt("_7"))
                .setDataUri(b.<Uri>getParcelable("_8"))
                .setArtworkUri(b.<Uri>getParcelable("_9"))
                .setMimeType(b.getString("_10"))
                .setHeaders(b.getString("_11"))
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Track)) return false;

        Track track = (Track) o;

        if (duration != track.duration) return false;
        if (albumArtistName != null ? !albumArtistName.equals(track.albumArtistName) : track.albumArtistName != null)
            return false;
        if (albumIdentity != null ? !albumIdentity.equals(track.albumIdentity) : track.albumIdentity != null)
            return false;
        if (albumName != null ? !albumName.equals(track.albumName) : track.albumName != null)
            return false;
        if (artistName != null ? !artistName.equals(track.artistName) : track.artistName != null)
            return false;
        if (artworkUri != null ? !artworkUri.equals(track.artworkUri) : track.artworkUri != null)
            return false;
        if (dataUri != null ?  !dataUri.equals(track.dataUri) : track.dataUri != null) return false;
        if (identity != null ? !identity.equals(track.identity) : track.identity != null)
            return false;
        if (name != null ? !name.equals(track.name) : track.name != null) return false;
        if (mimeType != null ? !mimeType.equals(track.mimeType) : track.name != null) return false;
        //ignoring headers

        return true;
    }

    @Override
    public int hashCode() {
        int result = identity != null ? identity.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (albumName != null ? albumName.hashCode() : 0);
        result = 31 * result + (artistName != null ? artistName.hashCode() : 0);
        result = 31 * result + (albumArtistName != null ? albumArtistName.hashCode() : 0);
        result = 31 * result + (albumIdentity != null ? albumIdentity.hashCode() : 0);
        result = 31 * result + duration;
        result = 31 * result + (dataUri != null ? dataUri.hashCode() : 0);
        result = 31 * result + (artworkUri != null ? artworkUri.hashCode() : 0);
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        //ignoring headers
        return result;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final BundleCreator<Track> BUNDLE_CREATOR = new BundleCreator<Track>() {
        @Override
        public Track fromBundle(Bundle b) throws IllegalArgumentException {
            return Track.fromBundle(b);
        }
    };

    public static final class Builder {
        private String identity;
        private String name;
        private String albumName;
        private String artistName;
        private String albumArtistName;
        private String albumIdentity;
        private int duration;
        private Uri dataUri;
        private Uri artworkUri;
        private String mimeType = DEFAULT_MIME_TYPE;
        private String headers = "";

        public Builder setIdentity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setAlbumName(String albumName) {
            this.albumName = albumName;
            return this;
        }

        public Builder setArtistName(String artistName) {
            this.artistName = artistName;
            return this;
        }

        public Builder setAlbumArtistName(String albumArtistName) {
            this.albumArtistName = albumArtistName;
            return this;
        }

        public Builder setAlbumIdentity(String albumIdentity) {
            this.albumIdentity = albumIdentity;
            return this;
        }

        public Builder setDuration(int duration) {
            this.duration = duration;
            return this;
        }

        public Builder setDataUri(Uri dataUri) {
            this.dataUri = dataUri;
            return this;
        }

        public Builder setArtworkUri(Uri artworkUri) {
            this.artworkUri = artworkUri;
            return this;
        }

        public Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        private Builder setHeaders(String headers) {
            this.headers = headers;
            return this;
        }

        public Builder addHeader(String key, String val) {
            String n = "";
            if (!TextUtils.isEmpty(headers)) {
                n += "\n";
            }
            headers += n + key + ":" + val;
            return this;
        }

        public Track build() {
            if (identity == null || name == null || dataUri == null) {
                throw new NullPointerException("identity, name, and dataUri are required");
            }
            return new Track(identity, name, albumName, artistName, albumArtistName,
                    albumIdentity, duration, dataUri, artworkUri, mimeType, headers);
        }
    }
}
