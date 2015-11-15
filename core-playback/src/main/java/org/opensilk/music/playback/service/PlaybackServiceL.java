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

package org.opensilk.music.playback.service;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaDescriptionCompat;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.music.index.client.IndexClient;
import org.opensilk.music.library.client.BundleableLoader;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.sort.BaseSortOrder;
import org.opensilk.music.model.sort.TrackSortOrder;
import org.opensilk.music.playback.PlaybackComponent;
import org.opensilk.music.playback.PlaybackConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by drew on 10/18/15.
 */
@TargetApi(21)
public class PlaybackServiceL extends MediaBrowserService implements PlaybackServiceProxy {

    @Inject PlaybackService mPlaybackService;

    final WeakHashMap<String, Subscription> mLoadChildrenSubscriptions = new WeakHashMap<>();

    @Override
    public void onCreate() {
        PlaybackComponent parent = DaggerService.getDaggerComponent(getApplicationContext());
        parent.playbackServiceComponent(PlaybackServiceModule.create(this)).inject(this);
        super.onCreate();
        mPlaybackService.onCreate(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPlaybackService.onDestroy();
        for (Subscription s : mLoadChildrenSubscriptions.values()) {
            RxUtils.unsubscribe(s);
        }
        mLoadChildrenSubscriptions.clear();
    }

    @Override
    @DebugLog
    public IBinder onBind(Intent intent) {
        mPlaybackService.onBind();
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mPlaybackService.onUnbind();
        return super.onUnbind(intent);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mPlaybackService.onTrimMemory(level);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return mPlaybackService.onStartCommand(intent, flags, startId);
    }

    @Override
    public void setSessionToken(Object token) {
        super.setSessionToken((MediaSession.Token) token);
    }

    @Override
    public void startSelf() {
        startService(new Intent(PlaybackServiceL.this, PlaybackServiceL.class));
    }

    @Override
    public MediaSessionHolder getSessionHolder() {
        return mPlaybackService.getSessionHolder();
    }

    @Override
    public Handler getHandler() {
        return mPlaybackService.getHandler();
    }

    @Override
    public Scheduler getScheduler() {
        return mPlaybackService.getScheduler();
    }

    @Nullable @Override //main thread
    @DebugLog
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        return new BrowserRoot(PlaybackConstants.AUTO_ROOT, null);
    }

    @Override //main thread
    @DebugLog
    public void onLoadChildren(@NonNull final String parentId, @NonNull final Result<List<MediaBrowser.MediaItem>> result) {
        result.detach();
        final Observable<List<MediaBrowser.MediaItem>> o;
        if (StringUtils.equals(PlaybackConstants.AUTO_ROOT, parentId)) {
            final IndexClient indexClient = mPlaybackService.getIndexClient();
            o = Observable.create(new Observable.OnSubscribe<List<MediaBrowser.MediaItem>>() {
                @Override
                public void call(Subscriber<? super List<MediaBrowser.MediaItem>> subscriber) {
                    List<MediaBrowser.MediaItem> list = new ArrayList<>();
                    for (MediaDescriptionCompat md : indexClient.getAutoRoots()) {
                        list.add(new MediaBrowser.MediaItem(
                                (MediaDescription)md.getMediaDescription(),
                                MediaBrowser.MediaItem.FLAG_BROWSABLE));
                    }
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(list);
                        subscriber.onCompleted();
                    }
                }
            }).subscribeOn(Schedulers.computation());
        } else {
            o = BundleableLoader.create(this)
                    .setUri(Uri.parse(parentId))
                    .setSortOrder(BaseSortOrder.A_Z)
                    .createObservable()
                    .flatMap(mBundleableListExpandFunc)
                    .map(mBundleableToMediaItemFunc)
                    .toList();
        }
        Subscription s = mLoadChildrenSubscriptions.remove(parentId);
        RxUtils.unsubscribe(s);
        s = o.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<MediaBrowser.MediaItem>>() {
                    @Override
                    public void call(List<MediaBrowser.MediaItem> mediaItems) {
                        result.sendResult(mediaItems);
                        mLoadChildrenSubscriptions.remove(parentId);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        result.sendResult(null);
                        mLoadChildrenSubscriptions.remove(parentId);
                    }
                });
        mLoadChildrenSubscriptions.put(parentId, s);
    }

    @Override
    @DebugLog
    public void onLoadItem(String itemId, final Result<MediaBrowser.MediaItem> result) {
        //TODO when is this used?
        super.onLoadItem(itemId, result);
    }

    final Func1<List<Bundleable>, Observable<Bundleable>> mBundleableListExpandFunc =
            new Func1<List<Bundleable>, Observable<Bundleable>>() {
                @Override
                public Observable<Bundleable> call(List<Bundleable> bundleables) {
                    return Observable.from(bundleables);
                }
    };

    final Func1<Bundleable, MediaBrowser.MediaItem> mBundleableToMediaItemFunc =
            new Func1<Bundleable, MediaBrowser.MediaItem>() {
                @Override
                public MediaBrowser.MediaItem call(Bundleable bundleable) {
                    if (bundleable instanceof Artist) {
                        Artist a = (Artist) bundleable;
                        return new MediaBrowser.MediaItem(
                                new MediaDescription.Builder()
                                        .setMediaId(a.getTracksUri().toString())
                                        .setTitle(a.getName())
                                        .setExtras(BundleHelper.b().putString(TrackSortOrder.ALBUM).get())
                                        .build(),
                                MediaBrowser.MediaItem.FLAG_PLAYABLE);
                    } else if (bundleable instanceof Album) {
                        Album a = (Album) bundleable;
                        return new MediaBrowser.MediaItem(
                                new MediaDescription.Builder()
                                        .setMediaId(a.getTracksUri().toString())
                                        .setTitle(a.getName())
                                        .setSubtitle(a.getArtistName())
                                        .setExtras(BundleHelper.b().putString(TrackSortOrder.PLAYORDER).get())
                                        .build(),
                                MediaBrowser.MediaItem.FLAG_PLAYABLE);
                    } else if (bundleable instanceof Genre) {
                        Genre g = (Genre) bundleable;
                        return new MediaBrowser.MediaItem(
                                new MediaDescription.Builder()
                                        .setMediaId(g.getTracksUri().toString())
                                        .setTitle(g.getName())
                                        .setExtras(BundleHelper.b().putString(TrackSortOrder.ALBUM).get())
                                        .build(),
                                MediaBrowser.MediaItem.FLAG_PLAYABLE);
                    } else if (bundleable instanceof Playlist) {
                        Playlist p = (Playlist) bundleable;
                        return new MediaBrowser.MediaItem(
                                new MediaDescription.Builder()
                                        .setMediaId(p.getTracksUri().toString())
                                        .setTitle(p.getName())
                                        .setExtras(BundleHelper.b().putString(TrackSortOrder.PLAYORDER).get())
                                        .build(),
                                MediaBrowser.MediaItem.FLAG_PLAYABLE);
                    } else {
                        throw new IllegalArgumentException("Unsupported bundleable " + bundleable.getClass());
                    }
                }
    };
}
