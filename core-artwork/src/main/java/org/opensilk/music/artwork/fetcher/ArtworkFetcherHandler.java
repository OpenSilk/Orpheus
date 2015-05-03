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
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.shared.ArtworkPreferences;
import org.opensilk.music.model.ArtInfo;

import org.opensilk.music.artwork.fetcher.ArtworkFetcherManager.CompletionListener;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Subscription;
import timber.log.Timber;

/**
 * Created by drew on 5/1/15.
 */
@ArtworkFetcherScope
public class ArtworkFetcherHandler extends Handler {

    public interface MSG {
        int NEW_INTENT = 1;
        int CLEAR_CACHES = 2;
        int RELOAD_PREFS = 3;
        int ON_LOW_MEM = 4;
    }

    WeakReference<ArtworkFetcherService> mService;
    final ArtworkFetcherManager mFetcherManager;
    final ArtworkPreferences mArtworkPrefs;

    @Inject
    public ArtworkFetcherHandler(
            @Named("fetcher")HandlerThread mHandlerThread,
            ArtworkFetcherManager mFetcherManager,
            ArtworkPreferences mArtworkPrefs
    ) {
        super(mHandlerThread.getLooper());
        this.mFetcherManager = mFetcherManager;
        this.mArtworkPrefs = mArtworkPrefs;
    }

    @Override
    public void handleMessage(Message msg) {
        final int startId = msg.arg1;
        switch (msg.what) {
            case MSG.NEW_INTENT: {
                Intent i = (Intent) msg.obj;
                Uri uri = i.getData();
                ArtInfo artInfo = i.getParcelableExtra(ArtworkFetcherService.EXTRA.ARTINFO);
                ArtworkType artworkType = ArtworkType.valueOf(i.getStringExtra(ArtworkFetcherService.EXTRA.ARTTYPE));
                Subscription s = mFetcherManager.fetch(uri, artInfo, artworkType, new CompletionListener() {
                    @Override
                    public void onComplete() {
                        stopService(startId);
                    }
                });
                //TODO keep subscriptions around?
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
                mFetcherManager.clearVolleyQueue();
                break;
            }
        }
    }

    void processIntent(Intent intent, int startId) {
        Timber.d("processIntent id=%d, i=%s", startId,intent);
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case ArtworkFetcherService.ACTION.NEWTASK:
                obtainMessage(MSG.NEW_INTENT, startId, 0, intent).sendToTarget();
                break;
            case ArtworkFetcherService.ACTION.CLEARCACHE:
                obtainMessage(MSG.CLEAR_CACHES, startId, 0).sendToTarget();
                break;
            case ArtworkFetcherService.ACTION.RELOADPREFS:
                obtainMessage(MSG.RELOAD_PREFS, startId, 0).sendToTarget();
                break;
        }
    }

    //Break Dependency cycle
    void setService(ArtworkFetcherService service) {
        mService = new WeakReference<ArtworkFetcherService>(service);
    }

    void onDestroy() {
        removeCallbacksAndMessages(null);
        mFetcherManager.onDestroy();
    }

    private void stopService(int startId) {
        ArtworkFetcherService s = mService != null ? mService.get() : null;
        if (s != null) s.maybeStopSelf(startId);
    }
}
