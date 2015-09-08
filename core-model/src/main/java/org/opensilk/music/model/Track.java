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
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.opensilk.music.model.spi.Bundleable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by drew on 6/10/14.
 */
public class Track extends Item {

    public static final String DEFAULT_MIME_TYPE = "audio/*";

    @Deprecated public final String albumName;
    @Deprecated public final String artistName;
    @Deprecated public final String albumArtistName;
    @Deprecated public final String albumIdentity;
    @Deprecated public final int duration;
    @Deprecated public final Uri dataUri;
    @Deprecated public final Uri artworkUri;
    @Deprecated public final String mimeType;
    @Deprecated public final int index;

    protected Track(@NonNull Uri uri, @NonNull String name, @NonNull Metadata metadata) {
        super(uri, name, metadata);
        this.albumName = getAlbumName();
        this.artistName = getArtistName();
        this.albumArtistName = getAlbumArtistName();
        this.albumIdentity = getAlbumUri().toString();
        this.duration = getDuration();
        this.dataUri = getResourceUri();
        this.artworkUri = getArtworkUri();
        this.mimeType = getMimeType();
        this.index = getPlayOrderIndex();
    }

    public String getAlbumName() {
        return metadata.getString(Metadata.KEY_ALBUM_NAME);
    }

    public String getAlbumArtistName() {
        return metadata.getString(Metadata.KEY_ALBUM_ARTIST_NAME);
    }

    public Uri getAlbumUri() {
        return metadata.getUri(Metadata.KEY_ALBUM_URI);
    }

    public String getArtistName() {
        return metadata.getString(Metadata.KEY_ARTIST_NAME);
    }

    public Uri getArtistUri() {
        return metadata.getUri(Metadata.KEY_ARTIST_URI);
    }

    public int getDuration() {
        return metadata.getInt(Metadata.KEY_DURATION);
    }

    public Uri getResourceUri() {
        return metadata.getUri(Metadata.KEY_RESOURCE_URI);
    }

    public Uri getArtworkUri() {
        return metadata.getUri(Metadata.KEY_ALBUM_ART_URI);
    }

    public String getMimeType() {
        final String mimeType = metadata.getString(Metadata.KEY_MIME_TYPE);
        return mimeType != null ? mimeType : DEFAULT_MIME_TYPE;
    }

    @NonNull
    public Map<String, String> getHeaders() {
        final String headers = metadata.getString(Metadata.KEY_RESOURCE_HEADERS);
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

    public int getPlayOrderIndex() {
        return metadata.getInt(Metadata.KEY_PLAY_ORDER_INDEX);
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, Track.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        return b;
    }

    protected static Track fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Track.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Track: "+b.getString(CLZ));
        }
        b.setClassLoader(Track.class.getClassLoader());
        return new Track(
                b.<Uri>getParcelable("_1"),
                b.getString("_2"),
                b.<Metadata>getParcelable("_3")
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder buildUpon() {
        return new Builder(this);
    }

    public static final BundleCreator<Track> BUNDLE_CREATOR = new BundleCreator<Track>() {
        @Override
        public Track fromBundle(Bundle b) throws IllegalArgumentException {
            return Track.fromBundle(b);
        }
    };

    public static final class Builder {
        private Uri uri;
        private String name;
        private Metadata.Builder bob = Metadata.builder();
        private String headers = "";

        private Builder() {
        }

        private Builder(Track t) {
            uri = t.uri;
            name = t.name;
            bob = t.metadata.buildUpon();
            headers = t.metadata.getString(Metadata.KEY_RESOURCE_HEADERS);
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

        public Builder setAlbumName(String albumName) {
            bob.putString(Metadata.KEY_ALBUM_NAME, albumName);
            return this;
        }

        public Builder setAlbumArtistName(String albumArtistName) {
            bob.putString(Metadata.KEY_ALBUM_ARTIST_NAME, albumArtistName);
            return this;
        }

        public Builder setAlbumUri(Uri albumUri) {
            bob.putUri(Metadata.KEY_ALBUM_URI, albumUri);
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

        public Builder setDuration(int duration) {
            bob.putInt(Metadata.KEY_DURATION, duration);
            return this;
        }

        public Builder setResourceUri(Uri resUri) {
            bob.putUri(Metadata.KEY_RESOURCE_URI, resUri);
            return this;
        }

        public Builder setArtworkUri(Uri artworkUri) {
            bob.putUri(Metadata.KEY_ALBUM_ART_URI, artworkUri);
            return this;
        }

        public Builder setMimeType(String mimeType) {
            bob.putString(Metadata.KEY_MIME_TYPE, mimeType);
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

        public Builder setPlayOrderIndex(int index) {
            bob.putInt(Metadata.KEY_PLAY_ORDER_INDEX, index);
            return this;
        }

        public Track build() {
            bob.putString(Metadata.KEY_RESOURCE_HEADERS, headers);
            Metadata metadata = bob.build();
            if (uri == null || name == null || metadata.getUri(Metadata.KEY_RESOURCE_URI) == null) {
                throw new NullPointerException("identity, name, and resourceUri are required");
            }
            return new Track(uri, name, metadata);
        }
    }
}
