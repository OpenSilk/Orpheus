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
import android.os.HandlerThread;
import android.os.IBinder;

import com.android.volley.RequestQueue;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.mortar.MortarService;
import org.opensilk.music.artwork.ArtworkComponent;
import org.opensilk.music.artwork.cache.BitmapCache;
import org.opensilk.music.artwork.cache.BitmapDiskCache;

import javax.inject.Inject;
import javax.inject.Named;

import mortar.MortarScope;

/**
 * Created by drew on 4/30/15.
 */
public class ArtworkFetcherService extends MortarService {

    public interface ACTION {
        String NEWTASK = "newtask";
        String CLEARCACHE = "clearcache";
    }

    public interface EXTRA {
        String ARTINFO = "artinfo";
        String ARTTYPE = "arttype";
    }

    @Inject @Named("fetcher") HandlerThread mHandlerThread;
    @Inject ArtworkFetcherHandler mHandler;

    @Override
    protected void onBuildScope(MortarScope.Builder builder) {
        builder.withService(DaggerService.DAGGER_SERVICE,
                ArtworkFetcherComponent.FACTORY.call(
                        DaggerService.<ArtworkComponent>getDaggerComponent(getApplicationContext()),
                        this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerService.<ArtworkFetcherComponent>getDaggerComponent(this).inject(this);
        //mHandlerThread.start(); //started by inject
        mHandler.setService(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
        mHandlerThread.getLooper().quit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.processIntent(intent, startId);
        return START_REDELIVER_INTENT;
    }
}
