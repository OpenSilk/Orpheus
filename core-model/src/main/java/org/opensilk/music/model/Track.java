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
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by drew on 6/10/14.
 */
public class Track extends Item {

    public static final String DEFAULT_MIME_TYPE = "audio/*";

    public static class Res implements Parcelable {
        private final Uri dataUri;
        private final Metadata metadata;

        protected Res(Uri dataUri, Metadata metadata) {
            this.dataUri = dataUri;
            this.metadata = metadata;
        }

        public Uri getUri() {
            return dataUri;
        }

        public String getMimeType() {
            final String mimeType = metadata.getString(Metadata.KEY_MIME_TYPE);
            return mimeType != null ? mimeType : DEFAULT_MIME_TYPE;
        }

        public long getBitrate() {
            return metadata.getLong(Metadata.KEY_BITRATE);
        }

        public long getSize() {
            return metadata.getLong(Metadata.KEY_SIZE);
        }

        public long getDuration() {
            return metadata.getLong(Metadata.KEY_DURATION);
        }

        public int getDurationS() {
            long duration = getDuration();
            return duration != 0 ? (int) (duration / 1000) : 0;
        }

        public long getLastMod() {
            return metadata.getLong(Metadata.KEY_LAST_MODIFIED);
        }

        public @NonNull Map<String, String> getHeaders() {
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

        public @Nullable String getHeaderString() {
            return metadata.getString(Metadata.KEY_RESOURCE_HEADERS);
        }

        public static Builder builder() {
            return new Builder();
        }

        public Builder buildUpon() {
            return new Builder(this);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(dataUri, flags);
            dest.writeParcelable(metadata, flags);
        }

        public static final Creator<Res> CREATOR = new Creator<Res>() {
            @Override
            public Res createFromParcel(Parcel source) {
                final Uri dataUri = source.readParcelable(Res.class.getClassLoader());
                final Metadata metadata = source.readParcelable(Res.class.getClassLoader());
                return new Res(dataUri, metadata);
            }

            @Override
            public Res[] newArray(int size) {
                return new Res[size];
            }
        };

        public static class Builder {
            private Uri dataUri;
            private Metadata.Builder bob = Metadata.builder();
            private String headers = "";

            private Builder() {
            }

            private Builder(Res res) {
                this.dataUri = res.dataUri;
                this.bob = res.metadata.buildUpon();
                this.headers = res.metadata.getString(Metadata.KEY_RESOURCE_HEADERS);
            }

            /**
             * Set uri pointing to data
             */
            public Builder setUri(Uri uri) {
                this.dataUri = uri;
                return this;
            }

            public Builder setMimeType(String mimeType) {
                bob.putString(Metadata.KEY_MIME_TYPE, mimeType);
                return this;
            }

            public Builder setBitrate(long bitrate) {
                bob.putLong(Metadata.KEY_BITRATE, bitrate);
                return this;
            }

            public Builder setSize(long size) {
                bob.putLong(Metadata.KEY_SIZE, size);
                return this;
            }

            public Builder setDuration(long duration) {
                bob.putLong(Metadata.KEY_DURATION, duration);
                return this;
            }

            public Builder setDurationS(int durationS) {
                bob.putLong(Metadata.KEY_DURATION, (long)durationS * 1000);
                return this;
            }

            /**
             * Opaque value must be > 1, increase when resource changes
             */
            public Builder setLastMod(long lastMod) {
                bob.putLong(Metadata.KEY_LAST_MODIFIED, lastMod);
                return this;
            }

            private Builder setHeaders(String headers) {
                this.headers = headers;
                return this;
            }

            public Builder addHeaders(Map<String, String> headers) {
                Set<Map.Entry<String, String>> es = headers.entrySet();
                for (Map.Entry<String, String> e : es) {
                    addHeader(e.getKey(), e.getValue());
                }
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

            public Res build() {
                bob.putString(Metadata.KEY_RESOURCE_HEADERS, headers);
                Metadata metadata = bob.build();
                if (dataUri == null) {
                    throw new NullPointerException("uri is required");
                }
                return new Res(dataUri, metadata);
            }

        }
    }

    private final ArrayList<Res> resList;

    protected Track(@NonNull Uri uri, @NonNull Uri parentUri, @NonNull String name,
                    @NonNull Metadata metadata, @NonNull ArrayList<Res> resList) {
        super(uri, parentUri, name, metadata);
        this.resList = resList;
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

    @Deprecated
    public long getDuration() {
        return metadata.getLong(Metadata.KEY_DURATION);
    }

    @Deprecated
    public int getDurationS() {
        long duration = getDuration();
        return duration != 0 ? (int) (duration / 1000) : 0;
    }

    public Uri getArtworkUri() {
        return metadata.getUri(Metadata.KEY_ALBUM_ART_URI);
    }

    public int getTrackNumber() {
        return metadata.getInt(Metadata.KEY_TRACK_NUMBER);
    }

    public int getDiscNumber() {
        return metadata.getInt(Metadata.KEY_DISC_NUMBER);
    }

    public boolean isCompilation() {
        return metadata.getInt(Metadata.KEY_IS_COMPILATION) > 0;
    }

    public String getGenre(){
        return metadata.getString(Metadata.KEY_GENRE_NAME);
    }

    public List<Res> getResources() {
        return Collections.unmodifiableList(resList);
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle(4);
        b.putString(CLZ, Track.class.getName());
        b.putParcelable("_1", uri);
        b.putString("_2", name);
        b.putParcelable("_3", metadata);
        b.putParcelableArrayList("_4", resList);
        b.putParcelable("_5", parentUri);
        return b;
    }

    protected static Track fromBundle(Bundle b) throws IllegalArgumentException {
        if (!Track.class.getName().equals(b.getString(CLZ))) {
            throw new IllegalArgumentException("Wrong class for Track: "+b.getString(CLZ));
        }
        b.setClassLoader(Track.class.getClassLoader());
        return new Track(
                b.<Uri>getParcelable("_1"),
                b.<Uri>getParcelable("_5"),
                b.getString("_2"),
                b.<Metadata>getParcelable("_3"),
                b.<Res>getParcelableArrayList("_4")
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
        private Uri parentUri;
        private String name;
        private Metadata.Builder bob = Metadata.builder();
        private ArrayList<Res> resList = new ArrayList<>();

        private Builder() {
        }

        private Builder(Track t) {
            uri = t.uri;
            parentUri = t.parentUri;
            name = t.name;
            bob = t.metadata.buildUpon();
            resList.addAll(t.getResources());
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

        @Deprecated
        public Builder setDuration(long duration) {
            bob.putLong(Metadata.KEY_DURATION, duration);
            return this;
        }

        public Builder setArtworkUri(Uri artworkUri) {
            bob.putUri(Metadata.KEY_ALBUM_ART_URI, artworkUri);
            return this;
        }

        public Builder setTrackNumber(int trackNumber) {
            bob.putInt(Metadata.KEY_TRACK_NUMBER, trackNumber);
            return this;
        }

        public Builder setDiscNumber(int discNumber) {
            bob.putInt(Metadata.KEY_DISC_NUMBER, discNumber);
            return this;
        }

        public Builder setIsCompliation(boolean yes) {
            bob.putInt(Metadata.KEY_IS_COMPILATION, yes ? 1 : 0);
            return this;
        }

        public Builder setGenre(String genre) {
            bob.putString(Metadata.KEY_GENRE_NAME, genre);
            return this;
        }

        public Builder setFlags(long flags) {
            bob.putLong(Metadata.KEY_FLAGS, flags);
            return this;
        }

        public Builder addRes(Res res) {
            resList.add(res);
            return this;
        }

        /**
         * @return modifiable list of resources
         */
        public List<Res> resList() {
            return resList;
        }

        public Track build() {
            if (uri == null || name == null || parentUri == null || resList.isEmpty()) {
                throw new NullPointerException("uri, parentUri, name, and resList are required");
            }
            return new Track(uri, parentUri, name, bob.build(), resList);
        }
    }
}
