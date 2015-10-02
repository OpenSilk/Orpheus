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
 * as specific subset of key value combinations. librarys should not need to use
 * this directly, as model classes provide getters/setters for metadata they
 * allow.
 *
 * Created by drew on 9/3/15.
 */
public class Metadata implements Parcelable {
    /**
     * String: Name of artist
     */
    public static final String KEY_ARTIST_NAME = "orpheus.artist.name";
    /**
     * String: Name of album
     */
    public static final String KEY_ALBUM_NAME = "orpheus.album.name";
    /**
     * String: Name album artist
     */
    public static final String KEY_ALBUM_ARTIST_NAME = "orpheus.album.artist.name";
    /**
     * Long: Year Album/track/etc was released
     */
    public static final String KEY_RELEASE_YEAR = "orpheus.release.year";
    /**
     * Long: (internal use) ms since epoch item was added to index
     */
    public static final String KEY_DATE_ADDED = "orpheus.date.added";
    /**
     * String: Human readable date object was last modified
     */
    public static final String KEY_DATE_MODIFIED = "orpheus.date.modified";
    /**
     * Long: Bitrate of {@link org.opensilk.music.model.Track.Res}
     */
    public static final String KEY_BITRATE = "orpheus.bitrate";
    /**
     * String: Mime type of {@link org.opensilk.music.model.Track.Res}
     */
    public static final String KEY_MIME_TYPE = "orpheus.mime.type";
    /**
     * Long: byte size of {@link org.opensilk.music.model.Track.Res}
     */
    public static final String KEY_SIZE = "orpheus.size";
    /**
     * Long: ms duration of {@link org.opensilk.music.model.Track.Res}
     */
    public static final String KEY_DURATION = "orpheus.duration";
    /**
     * Uri: (any scheme) where jpg/png/etc can be fetched containing cover art
     */
    public static final String KEY_ALBUM_ART_URI = "orpheus.album.art.uri";
    /**
     * Uri: (any scheme) where jpg/png/etc can be fetched containing image of artist or group
     */
    public static final String KEY_ARTIST_IMAGE_URI = "orpheus.artist.image.uri";
    /**
     * Uri: (content) library provider uri to {@link Album}
     */
    public static final String KEY_ALBUM_URI = "orpheus.album.uri";
    /**
     * Uri: (content) library provider uri to {@link Artist}
     */
    public static final String KEY_ARTIST_URI = "orpheus.artist.uri";
    /**
     * Uri: (any scheme) link to audio file for {@link Track.Res#getUri()}
     */
    public static final String KEY_RESOURCE_URI = "orpheus.resource.uri";
    /**
     * Uri: new line separated headers needed for accessing {@link #KEY_RESOURCE_URI} over http/s
     */
    public static final String KEY_RESOURCE_HEADERS = "orhpeus.resource.headers";
    /**
     * Uri: (content) library provider uri to parent {@link Container}
     */
    public static final String KEY_PARENT_URI = "orpheus.parent.uri";
    /**
     * Int: track count in {@link Container}
     */
    public static final String KEY_CHILD_TRACKS_COUNT = "orpheus.child.tracks.count";
    /**
     * Int: album count in {@link Container}
     */
    public static final String KEY_CHILD_ALBUMS_COUNT = "orpheus.child.albums.count";
    /**
     * Int: children of type {@link Container} or {@link Item} in this {@link Container}
     */
    public static final String KEY_CHILD_COUNT = "orpheus.child.count";
    /**
     * Uri: (content) library provider uri where a list of {@link Album}s
     *      can be fetched contained in this {@link TrackList}
     */
    public static final String KEY_CHILD_ALBUMS_URI = "orpheus.child.albums.uri";
    /**
     * Uri: (content) library provider uri where a last of {@link Track}s
     *      can be fetched contained in this {@link TrackList}
     */
    public static final String KEY_CHILD_TRACKS_URI = "orpheus.child.tracks.uri";
    /**
     * TODO try and remove this
     */
    public static final String KEY_ARTINFOS = "orpheus.artinfos";
    /**
     * Int: value > 0 if part of compilation
     */
    public static final String KEY_IS_COMPILATION = "orpheus.iscompilation";
    /**
     * String: name of genre this {@link Track} belongs to
     */
    public static final String KEY_GENRE_NAME = "orpheus.genre.name";
    /**
     * String: name/title of track
     */
    public static final String KEY_TRACK_NAME = "orpheus.track.name";
    /**
     * Int: position of track in album / playlist / etc
     */
    public static final String KEY_TRACK_NUMBER = "orpheus.track.number";
    /**
     * Int: disc number for track (defaults to 1)
     */
    public static final String KEY_DISC_NUMBER = "orpheus.disc.number";
    /**
     * Long: Opaque value provided by library provider, could be date, version, etc
     */
    public static final String KEY_LAST_MODIFIED = "orpheus.last.modified";
    /**
     * String: TODO still unsure if this should be used
     */
    public static final String KEY_SORT_NAME = "orpheus.sort.name";
    /**
     * Long: Bitmask of action flags in LibraryConfig
     */
    public static final String KEY_FLAGS = "orpheus.flags";
    /**
     * Uri: (content) library provider uri to a {@link Track}
     */
    public static final String KEY_TRACK_URI = "orpheus.track.uri";
    /**
     * String: artists musicbrainz id
     */
    public static final String KEY_ARTIST_MBID = "orpheus.artist.mbid";
    /**
     * String: bio/long deescription of artist
     */
    public static final String KEY_ARTIST_BIO = "orpheus.artist.bio";
    /**
     * String: brief summary of artist
     */
    public static final String KEY_ARTIST_SUMMARY = "orpheus.artist.summary";
    /**
     * Uri: (http) link to last.fm page
     */
    public static final String KEY_ARTIST_URL_URI = "orpheus.artist.url.uri";
    /**
     * String: artists musicbrainz id
     */
    public static final String KEY_ALBUM_MBID = "orpheus.album.mbid";
    /**
     * String: bio/long deescription of artist
     */
    public static final String KEY_ALBUM_BIO = "orpheus.album.bio";
    /**
     * String: brief summary of artist
     */
    public static final String KEY_ALBUM_SUMMARY = "orpheus.album.summary";
    /**
     * Uri: (http) link to last.fm page
     */
    public static final String KEY_ALBUM_URL_URI = "orpheus.album.url.uri";

    private final Bundle meta;

    private Metadata(Bundle b) {
        this.meta = b;
    }

    public String getString(String key) {
        return meta.getString(key, null);
    }

    public int getInt(String key) {
        return meta.getInt(key, -1);
    }

    public long getLong(String key) {
        return meta.getLong(key, -1);
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

    /**
     * TODO sanity check key types
     */
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
