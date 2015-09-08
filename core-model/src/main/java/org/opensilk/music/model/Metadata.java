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
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Common class for metadata to make parsing and future additions easier
 * Modeled after MediaMetadata. It is essentially a bundle that only allows
 * as specific subset of key value combinations. You should not need to use
 * this directly, as model classes provide getters/setters for metadata they
 * allow.
 *
 * Created by drew on 9/3/15.
 */
public class Metadata implements Parcelable {

    public static final String KEY_ARTIST_NAME = "orpheus.artist.name";
    public static final String KEY_ALBUM_NAME = "orpheus.album.name";
    public static final String KEY_ALBUM_ART_URI = "orpheus.album.art.uri";
    public static final String KEY_ARTIST_IMAGE_URI = "orpheus.artist.image.uri";
    public static final String KEY_YEAR = "orpheus.year";
    public static final String KEY_DATE_ADDED = "orpheus.date.added";
    public static final String KEY_DATE_MODIFIED = "orpheus.date.modified";
    public static final String KEY_BITRATE = "orpheus.bitrate";
    public static final String KEY_PLAY_ORDER_INDEX = "orpheus.play.order.index";
    public static final String KEY_MIME_TYPE = "orpheus.mime.type";
    public static final String KEY_SIZE = "orpheus.size";

    public static final String KEY_ALBUM_ARTIST_NAME = "orpheus.album.artist.name";
    public static final String KEY_DURATION = "orpheus.duration";
    public static final String KEY_ALBUM_URI = "orpheus.album.uri";
    public static final String KEY_ARTIST_URI = "orpheus.artist.uri";
    public static final String KEY_RESOURCE_URI = "orpheus.resource.uri";
    public static final String KEY_RESOURCE_HEADERS = "orhpeus.resource.headers";

    public static final String KEY_PARENT_URI = "orpheus.parent.uri";
    public static final String KEY_CHILD_TRACKS_COUNT = "orpheus.child.track.count";
    public static final String KEY_CHILD_ALBUMS_COUNT = "orpheus.child.album.count";
    public static final String KEY_CHILD_COUNT = "orpheus.child.count";
    public static final String KEY_CHILD_ALBUMS_URI = "orpheus.child.albums.uri";
    public static final String KEY_CHILD_TRACKS_URI = "orpheus.child.tracks.uri";
    public static final String KEY_ARTINFOS = "orpheus.artinfos";

    private final Bundle meta;

    private Metadata(Bundle b) {
        this.meta = b;
    }

    public String getString(String key) {
        return meta.getString(key);
    }

    public int getInt(String key) {
        return meta.getInt(key);
    }

    public long getLong(String key) {
        return meta.getLong(key);
    }

    public Uri getUri(String key) {
        return meta.getParcelable(key);
    }

    public List<ArtInfo> getArtInfos() {
        meta.setClassLoader(Metadata.class.getClassLoader());
        return meta.getParcelableArrayList(KEY_ARTINFOS);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder buildUpon() {
        return new Builder(meta);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(meta);
    }

    public static final Creator<Metadata> CREATOR = new Creator<Metadata>() {
        @Override
        public Metadata createFromParcel(Parcel source) {
            return new Metadata(source.readBundle(Metadata.class.getClassLoader()));
        }

        @Override
        public Metadata[] newArray(int size) {
            return new Metadata[size];
        }
    };

    public static class Builder {
        private final Bundle meta;

        private Builder() {
            meta = new Bundle();
        }

        private Builder(Bundle b) {
            this.meta = b;
        }

        public Builder putString(String key, String data) {
            meta.putString(key, data);
            return this;
        }

        public Builder putInt(String key, int data) {
            meta.putInt(key, data);
            return this;
        }

        public Builder putLong(String key, long data) {
            meta.putLong(key, data);
            return this;
        }

        public Builder putUri(String key, Uri data) {
            meta.putParcelable(key, data);
            return this;
        }

        public Builder putArtInfos(Collection<ArtInfo> artInfos) {
            meta.putParcelableArrayList(KEY_ARTINFOS, new ArrayList<Parcelable>(artInfos));
            return this;
        }

        public Metadata build() {
            return new Metadata(meta);
        }

    }

}
