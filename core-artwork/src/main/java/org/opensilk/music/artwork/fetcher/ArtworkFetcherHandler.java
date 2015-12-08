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

package org.opensilk.music.artwork.fetcher;

import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import org.opensilk.music.artwork.shared.ArtworkPreferences;
import org.opensilk.music.model.ArtInfo;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import javax.inject.Inject;

import rx.Subscription;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

/**
 * Created by drew on 5/1/15.
 */
@ArtworkFetcherScope
public class ArtworkFetcherHandler extends Handler {

    public interface MSG {
        int NEW_TASK = 1;
        int CLEAR_CACHES = 2;
        int RELOAD_PREFS = 3;
        int ON_LOW_MEM = 4;
        int CANCEL_TASK = 5;
    }

    final WeakReference<ArtworkFetcherService> mService;
    final ArtworkFetcherManager mFetcherManager;
    final ArtworkPreferences mArtworkPrefs;
    final WeakHashMap<String, Subscription> mActiveSubscriptions = new WeakHashMap<>();

    @Inject
    public ArtworkFetcherHandler(
            ArtworkFetcherService mService,
            ArtworkFetcherManager mFetcherManager,
            ArtworkPreferences mArtworkPrefs
    ) {
        super(mService.getHandlerThread().getLooper());
        this.mService = new WeakReference<>(mService);
        this.mFetcherManager = mFetcherManager;
        this.mArtworkPrefs = mArtworkPrefs;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG.NEW_TASK: {
                final Task task = (Task) msg.obj;
                task.listener.add(Subscriptions.create(new Action0() {
                    @Override
                    public void call() {
                        Timber.d("Removing task %s", task.artInfo.cacheKey());
                        synchronized (mActiveSubscriptions) {
                            mActiveSubscriptions.remove(task.artInfo.cacheKey());
                        }
                        stopService();
                    }
                }));
                Subscription s = mFetcherManager.fetch(task.artInfo, task.listener);
                synchronized (mActiveSubscriptions) {
                    mActiveSubscriptions.put(task.artInfo.cacheKey(), s);
                }
                break;
            } case MSG.CLEAR_CACHES: {
                mFetcherManager.clearCaches();
                stopService();
                break;
            }case MSG.RELOAD_PREFS: {
                mArtworkPrefs.reloadPrefs();
                stopService();
                break;
            }case MSG.ON_LOW_MEM: {
                synchronized (mActiveSubscriptions) {
                    for (Subscription s : mActiveSubscriptions.values()) {
                        if (s != null) {
                            s.unsubscribe();
                        }
                    }
                    mActiveSubscriptions.clear();
                }
                break;
            }case MSG.CANCEL_TASK: {
                ArtInfo artInfo = (ArtInfo) msg.obj;
                Subscription s = null;
                synchronized (mActiveSubscriptions) {
                    s = mActiveSubscriptions.remove(artInfo.cacheKey());
                }
                if (s != null) {
                    s.unsubscribe();
                }
                stopService();
                break;
            }
        }
    }

    void processIntent(Intent intent) {
        Timber.d("processIntent(%s)", intent);
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case ArtworkFetcherService.ACTION.NEWTASK: {
                if (intent.hasExtra(ArtworkFetcherService.EXTRA.ARTINFO)) {
                    intent.setExtrasClassLoader(ArtInfo.class.getClassLoader());
                    final ArtInfo artInfo = intent.getParcelableExtra(ArtworkFetcherService.EXTRA.ARTINFO);
                    final CompletionListener listener = new CompletionListener();
                    newTask(artInfo, listener);
                }
                break;
            }
            case ArtworkFetcherService.ACTION.CLEARCACHE:
                obtainMessage(MSG.CLEAR_CACHES).sendToTarget();
                break;
            case ArtworkFetcherService.ACTION.RELOADPREFS:
                obtainMessage(MSG.RELOAD_PREFS).sendToTarget();
                break;
        }
    }

    void newTask(ArtInfo artInfo, CompletionListener listener) {
        Timber.d("newTask(%s)", artInfo);
        obtainMessage(MSG.NEW_TASK, new Task(artInfo, listener)).sendToTarget();
    }

    void cancelTask(ArtInfo artInfo) {
        obtainMessage(MSG.CANCEL_TASK, artInfo).sendToTarget();
    }

    void onDestroy() {
        removeCallbacksAndMessages(null);
        mFetcherManager.onDestroy();
    }

    private void stopService() {
        synchronized (mActiveSubscriptions) {
            if (mActiveSubscriptions.isEmpty()) {
                ArtworkFetcherService s = mService != null ? mService.get() : null;
                if (s != null) s.maybeStopSelf();
            }
        }
    }

    private static final class Task {
        final ArtInfo artInfo;
        final CompletionListener listener;
        public Task(ArtInfo artInfo, CompletionListener listener) {
            this.artInfo = artInfo;
            this.listener = listener;
        }
    }

}
