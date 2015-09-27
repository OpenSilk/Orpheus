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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarIntentService;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.index.database.IndexSchema;
import org.opensilk.music.index.provider.LastFMHelper;
import org.opensilk.music.library.provider.LibraryExtras;
import org.opensilk.music.loader.BundleableLoader;
import org.opensilk.music.model.Container;
import org.opensilk.music.model.Metadata;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.index.scanner.NotificationHelper.Status;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Provider;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import timber.log.Timber;

import static org.opensilk.music.model.Metadata.*;

/**
 * Created by drew on 8/25/15.
 */
public class ScannerService extends MortarIntentService {

    public static final String ACTION_RESCAN = "rescan";
    public static final String EXTRA_AUTHORITY = "authority";
    public static final String EXTRA_LIBRARY_EXTRAS = "libraryextras";

    @Inject IndexDatabase mIndexDatabase;
    @Inject NotificationHelper mNotifHelper;
    @Inject MetaExtractor mMetaExtractor;
    @Inject Provider<BundleableLoader> mBundlelableLoaderProvider;

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
        String authority = intent.getStringExtra(EXTRA_AUTHORITY);
        if (ACTION_RESCAN.equals(intent.getAction())) {
            List<Pair<Uri, Uri>> topLevel = mIndexDatabase.findTopLevelContainers(authority);
            for (Pair<Uri,Uri> p : topLevel) {
                scan2(p.first, p.second);
            }
        } else {
            Bundle extras = intent.getBundleExtra(EXTRA_LIBRARY_EXTRAS);
            if (extras == null) {
                Timber.e("No extras in intent");
                return;
            }
            Container container = LibraryExtras.getBundleable(extras);
            if (container == null) {
                Timber.e("No container in extras");
                return;
            }
            scan2(container.getUri(), container.getParentUri());

        }
        notifSubject.onNext(Status.COMPLETED);
        notifSubject.onCompleted();
        mIndexDatabase.removeContainersInError(authority);
    }

    void scan2(final Uri uri, final Uri parentUri) {
        Timber.i("scan2(%s)", uri);
        BundleableLoader loader = mBundlelableLoaderProvider.get().setUri(uri);
        List<Bundleable> bundleables;
        try {
            bundleables = loader.createObservable().toBlocking().first();
        } catch (RuntimeException e) {
            //TODO first we should check if this is a transient failure
            mIndexDatabase.markContainerInError(uri);
            notifyError(uri);
            return;
        }
        mIndexDatabase.insertContainer(uri, parentUri);
        //first extract metadata from all tracks in container
        List<Pair<Track,Metadata>> trackMeta = new ArrayList<>(bundleables.size());
        for (Bundleable b : bundleables) {
            if (b instanceof Track) {
                numTotal.incrementAndGet();
                final Track item = (Track) b;
                if (mIndexDatabase.trackNeedsScan(item)) {
                    Track.Res res = item.getResources().get(0);
                    final Metadata meta = mMetaExtractor.extractMetadata(res);
                    trackMeta.add(Pair.create(item, meta));
                } else {
                    notifySkipped(item.getUri());
                }
            }
        }
        //Second fixup any descrepancies with albumartist/trackartist
        Set<String> albumArtists = new HashSet<>();
        Set<String> artists = new HashSet<>();
        for (Pair<Track,Metadata> pair : trackMeta) {
            String albumArtist = pair.second.getString(KEY_ALBUM_ARTIST_NAME);
            if (!StringUtils.isEmpty(albumArtist)) {
                albumArtists.add(albumArtist);
            }
            String artist = pair.second.getString(KEY_ARTIST_NAME);
            if (!StringUtils.isEmpty(artist)) {
                //strip of any featured artists
                artists.add(LastFMHelper.resolveAlbumArtistFromTrackArtist(artist));
            }
        }
        //not all the tracks had album artist set,
        //but we only have one artist, so use that as album artist for everyone
        if (albumArtists.size() != trackMeta.size() && artists.size() == 1) {
            final String artist = artists.toArray(new String[1])[0]; //FIXME ugly
            final List<Pair<Track,Metadata>> oldTrackMeta = new ArrayList<>();
            oldTrackMeta.addAll(trackMeta);
            trackMeta.clear();
            for (Pair<Track,Metadata> pair : oldTrackMeta) {
                Metadata m = pair.second.buildUpon()
                        .putString(KEY_ALBUM_ARTIST_NAME, artist)
                        .build();
                trackMeta.add(Pair.create(pair.first, m));
            }
        }
        //add everyone to the db
        for (Pair<Track,Metadata> pair : trackMeta) {
            final Track track = pair.first;
            final Metadata meta = pair.second;
            final boolean success =
                    mIndexDatabase.insertTrack(track, meta) > 0;
            if (success) {
                notifySuccess(track.getUri());
            } else {
                notifyError(track.getUri());
            }
        }
        //continue walking the tree
        for (Bundleable b : bundleables) {
            if (b instanceof Container) {
                Container c = (Container) b;
                scan2(c.getUri(), c.getParentUri());
            }
        }
    }

}
