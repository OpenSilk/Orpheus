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
import org.opensilk.music.model.ArtInfo;

import org.opensilk.music.artwork.fetcher.ArtworkFetcherManager.CompletionListener;

import java.lang.ref.WeakReference;

import javax.inject.Inject;
import javax.inject.Named;

import rx.Subscription;

/**
 * Created by drew on 5/1/15.
 */
@ArtworkFetcherScope
public class ArtworkFetcherHandler extends Handler {

    public interface MSG {
        int NEW_INTENT = 1;
        int CLEAR_CACHES = 2;
    }

    WeakReference<ArtworkFetcherService> mService;
    final ArtworkFetcherManager mFetcherManager;

    @Inject
    public ArtworkFetcherHandler(
            @Named("fetcher")HandlerThread mHandlerThread,
            ArtworkFetcherManager mFetcherManager
    ) {
        super(mHandlerThread.getLooper());
        this.mFetcherManager = mFetcherManager;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case MSG.NEW_INTENT: {
                final int startId = msg.arg1;
                Intent i = (Intent) msg.obj;
                Uri uri = i.getData();
                ArtInfo artInfo = i.getParcelableExtra(ArtworkFetcherService.EXTRA.ARTINFO);
                ArtworkType artworkType = ArtworkType.valueOf(i.getStringExtra(ArtworkFetcherService.EXTRA.ARTTYPE));
                Subscription s = mFetcherManager.fetch(uri, artInfo, artworkType, new CompletionListener() {
                    @Override
                    public void onComplete() {
                        ArtworkFetcherService s = mService != null ? mService.get() : null;
                        if (s != null) s.stopSelf(startId);
                    }
                });
                //TODO keep subscriptions around?
                break;
            } case MSG.CLEAR_CACHES: {
                mFetcherManager.clearCaches();
                break;
            }
        }
    }

    //Break Dependency cycle
    void setService(ArtworkFetcherService service) {
        mService = new WeakReference<ArtworkFetcherService>(service);
    }

    void processIntent(Intent intent, int startId) {
        if (intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        switch (action) {
            case ArtworkFetcherService.ACTION.NEWTASK:
                obtainMessage(MSG.NEW_INTENT, startId, 0, intent).sendToTarget();
                break;
            case ArtworkFetcherService.ACTION.CLEARCACHE:
                sendEmptyMessage(MSG.CLEAR_CACHES);
                break;
        }
    }
}
