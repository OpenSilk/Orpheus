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

package org.opensilk.music.index.scanner;

import android.app.Service;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.VolleyError;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.music.library.internal.BundleableListSlice;
import org.opensilk.music.library.internal.IBundleableObserver;
import org.opensilk.music.library.internal.LibraryException;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.inject.Inject;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.LastFM;
import de.umass.lastfm.MusicEntry;
import de.umass.lastfm.MusicEntryResponseCallback;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Func2;
import timber.log.Timber;

/**
 * Created by drew on 8/25/15.
 */
public class ScannerService extends Service {

    @Inject LastFM mLastFM;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    TreeSet<Bundleable> tree = new TreeSet<>(new Comparator<Bundleable>() {
        @Override
        public int compare(Bundleable lhs, Bundleable rhs) {
            return ObjectUtils.compare(lhs.getIdentity(), rhs.getIdentity());
        }
    });

    public void scan(String authority, String library, String containerId) {
        Uri uri = LibraryUris.browse(authority, library, containerId);
        List<Bundleable> list;
        try {
            list = createObservable(uri).toBlocking().first();
        } catch (RuntimeException e) {
            Timber.w(e, e.getClass().getSimpleName());
            return;
        }
        for (Bundleable b : list) {
            if (b instanceof Track) {
                Track t = (Track) b;
            } else if (b instanceof Folder) {
                scan(authority, library, b.getIdentity());
            }
        }
    }

    Metadata extractMeta(Uri uri, Map<String, String> headers) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            if (StringUtils.startsWith(uri.getScheme(), "http")) {
                mmr.setDataSource(uri.toString(), headers != null ? headers : new HashMap<String, String>());
            } else {
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "r");
                mmr.setDataSource(pfd.getFileDescriptor());
            }

            Metadata.Builder metabuilder = Metadata.builder()
                    .putString(Metadata.KEY_ALBUM_NAME, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
                    .putString(Metadata.KEY_ALBUM_ARTIST_NAME, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
                    .putString(Metadata.KEY_ARTIST_NAME, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                    .putString(Metadata.KEY_GENRE_NAME, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE))
                    .putString(Metadata.KEY_MIME_TYPE, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE))
                    .putString(Metadata.KEY_TRACK_NAME, mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE))
                    ;
            final String bitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            if (bitrate != null) {
                try {
                    metabuilder.putLong(Metadata.KEY_BITRATE, Long.parseLong(bitrate));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_BITRATE)");
                }
            }
            final String track_num = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            if (track_num != null) {
                try {
                    if (StringUtils.contains(track_num, "/")) {
                        metabuilder.putInt(Metadata.KEY_TRACK_NUMBER, Integer.parseInt(StringUtils.split(track_num, "/")[0]));
                    } else {
                        metabuilder.putInt(Metadata.KEY_TRACK_NUMBER, Integer.parseInt(track_num));
                    }
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(CD_TRACK_NUMBER)");
                }
            }
            final String disc_num = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            if (disc_num != null) {
                try {
                    if (StringUtils.contains(disc_num, "/")) {
                        metabuilder.putInt(Metadata.KEY_DISC_NUMBER, Integer.parseInt(StringUtils.split(disc_num, "/")[0]));
                    } else {
                        metabuilder.putInt(Metadata.KEY_DISC_NUMBER, Integer.parseInt(disc_num));
                    }
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(DISC_NUMBER)");
                }
            }
            final String compilation = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION);
            if (compilation != null) {
                try {
                    metabuilder.putInt(Metadata.KEY_IS_COMPILATION, Integer.parseInt(compilation));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_COMPILATION)");
                }
            }
            final String duration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (duration != null) {
                try {
                    metabuilder.putInt(Metadata.KEY_DURATION, Integer.parseInt(duration));
                } catch (NumberFormatException e) {
                    Timber.w(e, "extractMeta(KEY_DURATION)");
                }
            }
            return metabuilder.build();
        } catch (Exception e) { //setDataSource throws runtimeException
            Timber.e(e, "extractMeta");
        } finally {
            mmr.release();
        }
        return null;
    }

    void lookupInfo(Uri uri, Metadata meta) {

        String albumArtist = meta.getString(Metadata.KEY_ALBUM_ARTIST_NAME);
        if (StringUtils.isEmpty(albumArtist)) {
            //No albumartist, we still may get away with the track artist though.
            albumArtist = meta.getString(Metadata.KEY_ARTIST_NAME);
            //Artist names such as "Pretty Lights feat. Eligh" will fail so strip off the featured artist.
            if (StringUtils.containsIgnoreCase(albumArtist, "feat.")) {
                String[] strings = StringUtils.splitByWholeSeparator(albumArtist.toLowerCase(), "feat.");
                if (strings.length > 0) {
                    albumArtist = strings[0].trim();
                }
            }
        }

        String albumName = meta.getString(Metadata.KEY_ALBUM_NAME);

        if (StringUtils.isEmpty(albumArtist) || StringUtils.isEmpty(albumName)) {
            Timber.w("Cannot process song %s", uri);
            return;
        }

        Observable<Album> albumObservable = mLastFM.newAlbumRequestObservable(albumArtist, albumName);
        Observable<Artist> albumArtistObservable = mLastFM.newArtistRequestObservable(albumArtist);

        String artist = meta.getString(Metadata.KEY_ARTIST_NAME);
        //Artist names such as "Pretty Lights feat. Eligh" will fail so strip off the album artist.
        if (StringUtils.containsIgnoreCase(artist, "feat.")) {
            String[] strings = StringUtils.splitByWholeSeparator(artist.toLowerCase(), "feat.");
            if (strings.length > 1) {
                artist = strings[2].trim();
            }
        }

        Observable<MusicEntry> meObservable;

        if (StringUtils.equalsIgnoreCase(albumArtist, artist)) {
            meObservable = Observable.mergeDelayError(albumObservable, albumArtistObservable);
        } else {
            Observable<Artist> artistObservable = mLastFM.newArtistRequestObservable(artist);
            meObservable = Observable.mergeDelayError(albumObservable, albumArtistObservable, artistObservable);
        }



    }

    public Observable<List<Bundleable>> createObservable(final Uri uri) {
        return Observable.create(new Observable.OnSubscribe<List<Bundleable>>() {
            @Override
            public void call(final Subscriber<? super List<Bundleable>> subscriber) {
                final IBundleableObserver o = new IBundleableObserver.Stub() {
                    @Override
                    public void onNext(BundleableListSlice slice) throws RemoteException {
                        List<Bundleable> list = new ArrayList<>(slice.getList());
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onNext(list);
                        }
                    }

                    @Override
                    public void onError(LibraryException e) throws RemoteException {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onError(e);
                        }
                    }

                    @Override
                    public void onCompleted() throws RemoteException {
                        if (!subscriber.isUnsubscribed()) {
                            subscriber.onCompleted();
                        }
                    }
                };

                final Bundle extras = LibraryExtras.b()
                        .putUri(uri)
                        .putBundleableObserverCallback(o)
                        .get();

                Bundle ok = getContentResolver().call(uri, "scan", null, extras);
                if (!LibraryExtras.getOk(ok)) {
                    subscriber.onError(LibraryExtras.getCause(ok));
                }
            }
        });
    }
}
