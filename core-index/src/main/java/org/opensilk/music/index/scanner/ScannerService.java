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

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarIntentService;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.index.database.IndexSchema;
import org.opensilk.music.index.provider.LastFMHelper;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.index.scanner.NotificationHelper.Status;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import de.umass.lastfm.Album;
import de.umass.lastfm.Artist;
import de.umass.lastfm.LastFM;
import hugo.weaving.DebugLog;
import mortar.MortarScope;
import retrofit.Call;
import retrofit.Response;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import timber.log.Timber;

import static android.provider.MediaStore.Audio.keyFor;
import static org.opensilk.common.core.util.Preconditions.checkNotNullOrBlank;

import static org.opensilk.music.model.Metadata.*;

/**
 * Created by drew on 8/25/15.
 */
public class ScannerService extends MortarIntentService {

    @Inject LastFMHelper mLastFM;
    @Inject IndexDatabase mIndexDatabase;
    @Inject NotificationHelper mNotifHelper;
    @Inject MetaExtractor mMetaExtractor;

    final AtomicInteger numTotal = new AtomicInteger(0);
    final AtomicInteger numError = new AtomicInteger(0);
    final AtomicInteger numProcessed = new AtomicInteger(0);

    Subject<Status, Status> notifSubject;//DO NOT ACCESS FROM MAIN THREAD

    public ScannerService() {
        super(ScannerService.class.getSimpleName());
    }

    @Override
    protected void onBuildScope(MortarScope.Builder builder) {
        IndexComponent acc = DaggerService.getDaggerComponent(getApplicationContext());
        builder.withService(DaggerService.DAGGER_SERVICE, ScannerComponent.FACTORY.call(acc));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ScannerComponent acc = DaggerService.getDaggerComponent(this);
        acc.inject(this);

        mNotifHelper.attachService(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNotifHelper.detachService(this);
    }

    void notifySuccess(Uri uri) {
        Timber.v("Indexed %s", uri);
        numProcessed.incrementAndGet();
        notifSubject.onNext(Status.SCANNING);
    }

    void notifySkipped(Uri uri) {
        Timber.d("Skipping item already in db %s", uri);
        numProcessed.incrementAndGet();
        notifSubject.onNext(Status.SCANNING);
    }

    void notifyError(Uri uri) {
        Timber.w("An error occured while proccessing %s", uri);
        numError.incrementAndGet();
        notifSubject.onNext(Status.SCANNING);
    }

    void updateNotification(Status status) {
        mNotifHelper.updateNotification(status, numProcessed.get(), numError.get(), numTotal.get());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getBundleExtra(LibraryExtras.INTENT_KEY);
        if (extras == null) {
            Timber.e("No extras in intent");
            return;
        }
        Container container = LibraryExtras.getBundleable(extras);
        if (container == null) {
            Timber.e("No container in extras");
            return;
        }
        notifSubject = PublishSubject.create();
        notifSubject.asObservable().debounce(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Status>() {
                    @Override
                    @DebugLog
                    public void call(Status status) {
                        updateNotification(status);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    @DebugLog
                    public void call(Throwable throwable) {
                        updateNotification(Status.COMPLETED);
                    }
                });
        notifSubject.onNext(Status.SCANNING);
        scan2(container.getUri(), container.getParentUri());
        notifSubject.onNext(Status.COMPLETED);
        notifSubject.onCompleted();
    }

    void scan2(final Uri uri, final Uri parentUri) {
        Timber.i("scan2(%s)", uri);
        BundleableLoader loader = new BundleableLoader(this, uri, null);
        List<Bundleable> bundleables;
        try {
            bundleables = loader.createObservable().toBlocking().first();
        } catch (RuntimeException e) {
            notifyError(uri);
            return;
        }
        mIndexDatabase.insertContainer(uri, parentUri);
        for (Bundleable b : bundleables) {
            if (b instanceof Track) {
                numTotal.incrementAndGet();
                Track item = (Track) b;
                Timber.i("indexing %s, %s", item.getName(), item.getUri());
                if (!needsScan(item)) {
                    notifySkipped(item.getUri());
                    continue;
                }
                boolean success = false;
                Metadata meta = extractMeta(item);
                if (meta != null) {
                    success = addMetaToDb(meta);
                }
                if (success) {
                    notifySuccess(item.getUri());
                } else {
                    notifyError(item.getUri());
                }
            } else if (b instanceof Container) {
                Container c = (Container) b;
                scan2(c.getUri(), c.getParentUri());
            } else {
                Timber.w("Unsupported bundleable %s at %s", b.getClass(), b.getUri());
            }
        }
    }

    Metadata extractMeta(Track track) {
        Track.Res res = track.getResources().get(0);
        Metadata meta = mMetaExtractor.extractMetadata(res);
        return meta.buildUpon()
                .putUri(KEY_TRACK_URI, track.getUri())
                .putUri(KEY_PARENT_URI, track.getParentUri())
                .putLong(KEY_SIZE, res.getSize())
                .putString(KEY_MIME_TYPE, res.getMimeType())
                .putLong(KEY_LAST_MODIFIED, res.getLastMod())
                .build();
    }

    boolean addMetaToDb(Metadata meta) {

        final Uri trackUri = meta.getUri(KEY_TRACK_URI);

        final String albumArtistName = meta.getString(KEY_ALBUM_ARTIST_NAME);
        final String albumName = meta.getString(KEY_ALBUM_NAME);
        final String artistName = meta.getString(KEY_ARTIST_NAME);

        if (StringUtils.isEmpty(albumArtistName)) {
            Timber.w("Cannot process item %s missing albumArtistName", trackUri);
            return false;
        } else if (StringUtils.isEmpty(albumName)) {
            Timber.w("Cannot process item %s missing albumName", trackUri);
            return false;
        } else if (StringUtils.isEmpty(artistName)) {
            Timber.w("Cannot process item %s missing artistName", trackUri);
            return false;
        }

        long albumArtistId = mIndexDatabase.hasArtist(albumArtistName);
        if (albumArtistId < 0) {
            Metadata artistMeta = mLastFM.lookupArtistInfo(albumArtistName);
            if (artistMeta == null) {
                //Straight lookup failed, maybe we can fudge it.
                artistMeta = mLastFM.lookupArtistInfo(
                        LastFMHelper.resolveAlbumArtistFromTrackArtist(artistName));
            }
            if (artistMeta != null) {
                albumArtistId = mIndexDatabase.insertArtist(artistMeta);
            }
            if (albumArtistId < 0) {
                Timber.e("Unable to insert artist %s into db", albumArtistName);
                return false;
            }
        }

        long albumId = mIndexDatabase.hasAlbum(albumArtistName, albumName);
        if (albumId < 0) {
            Metadata albumMeta = mLastFM.lookupAlbumInfo(albumArtistName, albumName);
            if (albumMeta != null) {
                albumId = mIndexDatabase.insertAlbum(albumMeta, albumArtistId);
            }
            if (albumId < 0) {
                Timber.e("Unable to insert album %s into db", albumName);
                return false;
            }
        }

        long artistId;
        if (StringUtils.equalsIgnoreCase(artistName, albumArtistName)) {
            artistId = albumArtistId;
        } else {
            artistId = mIndexDatabase.hasArtist(artistName);
            if (artistId < 0) {
                Metadata artistMeta = mLastFM.lookupArtistInfo(artistName);
                if (artistMeta == null) {
                    artistMeta = mLastFM.lookupArtistInfo(
                            LastFMHelper.resolveTrackArtist(artistName));
                }
                if (artistMeta != null) {
                    artistId = mIndexDatabase.insertArtist(artistMeta);
                }
                if (artistId < 0) {
                    Timber.e("Unable to insert artist %s into db", artistName);
                    return false;
                }
            }
        }

        long containerId = mIndexDatabase.hasContainer(meta.getUri(KEY_PARENT_URI));
        if (containerId < 0) {
            Timber.e("Unable locate parent for track %s", meta.getUri(KEY_TRACK_NAME));
            return false;
        }

        long trackId = mIndexDatabase.insertTrackResource(meta, artistId, albumId, containerId);
        if ( trackId <0) {
            Timber.e("Unable to insert track %s into db", meta.getUri(KEY_TRACK_NAME));
            return false;
        }

        //TODO artwork

        return true;
    }

    private static final String[] resMetaCols = new String[] {
            IndexSchema.TrackResMeta.SIZE,
            IndexSchema.TrackResMeta.LAST_MOD,
    };

    @DebugLog
    boolean needsScan(Track track) {
        Cursor c = null;
        try {
            final Track.Res res = track.getResources().get(0);
            final String sel = IndexSchema.TrackResMeta.URI + "=?";
            final String[] selArgs = new String[] {track.getUri().toString()};
            c = mIndexDatabase.query(IndexSchema.TrackResMeta.TABLE, resMetaCols, sel, selArgs, null, null, null);
            if (c != null && c.moveToFirst()) {
                final long size = c.getLong(0);
                final long lastMod = c.getLong(1);
                //if size or lastmod has changed we need to rescan
                if (res.getSize() != size || res.getLastMod() != lastMod) {
                    mIndexDatabase.delete(IndexSchema.TrackResMeta.TABLE, sel, selArgs);
                    return true;
                }
                return false;
            }
            return true;
        } finally {
            closeCursor(c);
        }
    }

    static void closeCursor(Cursor c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

}
