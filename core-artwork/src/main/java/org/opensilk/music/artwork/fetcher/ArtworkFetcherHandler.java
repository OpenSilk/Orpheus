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

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
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
    }

    final WeakReference<ArtworkFetcherService> mService;
    final ArtworkFetcherManager mFetcherManager;
    final ArtworkPreferences mArtworkPrefs;
    final CompositeSubscription mSubscriptions = new CompositeSubscription();

    @Inject
    public ArtworkFetcherHandler(
            ArtworkFetcherService mService,
            ArtworkFetcherManager mFetcherManager,
            ArtworkPreferences mArtworkPrefs
    ) {
        super(mService.getHandlerThread().getLooper());
        this.mService = new WeakReference<ArtworkFetcherService>(mService);
        this.mFetcherManager = mFetcherManager;
        this.mArtworkPrefs = mArtworkPrefs;
    }

    @Override
    public void handleMessage(Message msg) {
        final int startId = msg.arg1;
        switch (msg.what) {
            case MSG.NEW_TASK: {
                Task task = (Task) msg.obj;
                Subscription s = mFetcherManager.fetch(task.artInfo, task.listener);
                if (msg.arg1 == 1) {
                    mSubscriptions.add(s);
                }
                break;
            } case MSG.CLEAR_CACHES: {
                mFetcherManager.clearCaches();
                stopService(startId);
                break;
            }case MSG.RELOAD_PREFS: {
                mArtworkPrefs.reloadPrefs();
                stopService(startId);
                break;
            }case MSG.ON_LOW_MEM: {
                mSubscriptions.clear();
                break;
            }
        }
    }

    void processIntent(Intent intent, final int startId) {
        Timber.d("processIntent id=%d, i=%s", startId,intent);
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case ArtworkFetcherService.ACTION.NEWTASK: {
                if (!intent.hasExtra(ArtworkFetcherService.EXTRA.ARTINFO)) return;
                final ArtInfo artInfo = intent.getParcelableExtra(ArtworkFetcherService.EXTRA.ARTINFO);
                CompletionListener listener = new CompletionListener() {
                    @Override public void onError(Throwable e) {
                        onDone();
                    }
                    @Override public void onCompleted() {
                        onDone();
                    }
                    void onDone() {
                        mSubscriptions.remove(this);
                        Timber.d("Has subscriptions=%d", mSubscriptions.hasSubscriptions());
                        stopService(startId);
                    }
                };
                newTaskInternal(artInfo, listener, true);
                break;
            }
            case ArtworkFetcherService.ACTION.CLEARCACHE:
                obtainMessage(MSG.CLEAR_CACHES, startId, 0).sendToTarget();
                break;
            case ArtworkFetcherService.ACTION.RELOADPREFS:
                obtainMessage(MSG.RELOAD_PREFS, startId, 0).sendToTarget();
                break;
        }
    }

    @DebugLog
    void newTask(ArtInfo artInfo, CompletionListener listener) {
        newTaskInternal(artInfo, listener, false);
    }

    @DebugLog
    private void newTaskInternal(ArtInfo artInfo, CompletionListener listener, boolean track) {
        obtainMessage(MSG.NEW_TASK, track ? 1 : 0, 0, new Task(artInfo, listener)).sendToTarget();
    }

    void onDestroy() {
        removeCallbacksAndMessages(null);
        mFetcherManager.onDestroy();
    }

    private void stopService(int startId) {
        ArtworkFetcherService s = mService != null ? mService.get() : null;
        if (s != null) s.maybeStopSelf(startId);
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
