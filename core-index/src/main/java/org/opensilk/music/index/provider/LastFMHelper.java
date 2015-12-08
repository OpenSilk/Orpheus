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

package org.opensilk.music.index.provider;

import android.net.Uri;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.index.model.BioContent;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.LastFM;
import hugo.weaving.DebugLog;
import retrofit.Call;
import retrofit.Response;
import rx.Observable;
import rx.Subscriber;
import timber.log.Timber;

import static android.provider.MediaStore.Audio.keyFor;
import static org.opensilk.music.model.Metadata.*;

/**
 * Created by drew on 9/21/15.
 */
@Singleton
public class LastFMHelper {

    final LastFM mLastFM;

    @Inject
    public LastFMHelper(LastFM mLastFM) {
        this.mLastFM = mLastFM;
    }

    @DebugLog
    public Metadata lookupAlbumInfo(final String albumArtist, final String albumName) {
        Call<Album> call = mLastFM.getAlbum(albumArtist, albumName);
        Response<Album> response;
        try {
            response = call.execute();
        } catch (IOException|IllegalArgumentException e) {
            return null;
        }
        Album album = response.body();
        if (album == null) {
            Timber.w("Failed to retrieve album %s", albumName);
            return null;
        }
        Metadata.Builder bob = Metadata.builder();
        bob.putString(KEY_ALBUM_NAME, album.getName());
        bob.putString(KEY_ALBUM_ARTIST_NAME, album.getArtist());
        bob.putString(KEY_ALBUM_SUMMARY, album.getWikiSummary());
        bob.putString(KEY_ALBUM_BIO, album.getWikiText());
        Date lastChanged = album.getWikiLastChanged();
        if (lastChanged != null) {
            bob.putLong(KEY_LAST_MODIFIED, lastChanged.getTime());
        }
        bob.putUri(KEY_ALBUM_URL_URI, Uri.parse(album.getUrl()));
        bob.putString(KEY_ALBUM_MBID, album.getMbid());
        return bob.build();
    }

    @DebugLog
    public Metadata lookupArtistInfo(final String artistName) {
        Call<Artist> call = mLastFM.getArtist(artistName);
        Response<Artist> response;
        try {
            response = call.execute();
        } catch (IOException|IllegalArgumentException e) {
            return null;
        }
        Artist artist = response.body();
        if (artist == null) {
            Timber.w("Failed to retrieve artist %s", artistName);
            return null;
        }
        Metadata.Builder bob = Metadata.builder();
        bob.putString(KEY_ARTIST_NAME, artist.getName());
        bob.putString(KEY_ARTIST_SUMMARY, artist.getWikiSummary());
        bob.putString(KEY_ARTIST_BIO, artist.getWikiText());
        Date lastChanged = artist.getWikiLastChanged();
        if (lastChanged != null) {
            bob.putLong(KEY_LAST_MODIFIED, lastChanged.getTime());
        }
        bob.putUri(KEY_ARTIST_URL_URI, Uri.parse(artist.getUrl()));
        bob.putString(KEY_ARTIST_MBID, artist.getMbid());
        return bob.build();
    }

    public Observable<Album> getAlbum(final String mbid) {
        return Observable.create(new Observable.OnSubscribe<Album>() {
            @Override
            public void call(Subscriber<? super Album> subscriber) {
                Call<Album> call = mLastFM.getAlbumbyMbid(mbid);
                Album album;
                try {
                    album = call.execute().body();
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(album);
                        subscriber.onCompleted();
                    }
                } catch (IOException|IllegalArgumentException e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    public Observable<Artist> getArtist(final String mbid) {
        return Observable.create(new Observable.OnSubscribe<Artist>() {
            @Override
            public void call(Subscriber<? super Artist> subscriber) {
                Call<Artist> call = mLastFM.getArtistbyMbid(mbid);
                try {
                    Artist artist = call.execute().body();
                    if(!subscriber.isUnsubscribed()) {
                        subscriber.onNext(artist);
                        subscriber.onCompleted();
                    }
                } catch (IOException|IllegalArgumentException e) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(e);
                    }
                }
            }
        });
    }

    public static boolean hasFeaturedArtist(String artist) {
        return StringUtils.containsIgnoreCase(artist, "feat.");
    }

    public static String resolveAlbumArtistFromTrackArtist(String trackArtist) {
        if (StringUtils.containsIgnoreCase(trackArtist, "feat.")) {
            String[] strings = StringUtils.splitByWholeSeparator(trackArtist.toLowerCase(), "feat.");
            if (strings.length > 0) {
                return strings[0].trim();
            }
        }
        return trackArtist;
    }

    public static String resolveTrackArtist(String trackArtist) {
        if (StringUtils.containsIgnoreCase(trackArtist, "feat.")) {
            String[] strings = StringUtils.splitByWholeSeparator(trackArtist.toLowerCase(), "feat.");
            if (strings.length > 1) {
                return strings[2].trim();
            }
        }
        return trackArtist;
    }
}
