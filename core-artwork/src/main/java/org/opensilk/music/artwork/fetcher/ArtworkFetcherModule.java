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

import android.content.Context;
import android.content.UriMatcher;
import android.os.Handler;
import android.os.HandlerThread;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import org.opensilk.common.core.app.BaseApp;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.SchedulerResponseDelivery;
import org.opensilk.music.artwork.cache.CacheUtil;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import static org.opensilk.music.artwork.Constants.*;

/**
 * Created by drew on 5/1/15.
 */
@Module
public class ArtworkFetcherModule {

    final ArtworkFetcherService service;

    public ArtworkFetcherModule(ArtworkFetcherService service) {
        this.service = service;
    }

    @Provides @ArtworkFetcherScope
    public ArtworkFetcherService provideService() {
        return service;
    }

    @Provides @ArtworkFetcherScope
    public RequestQueue provideRequestQueue(@ForApplication Context context) {
        final int poolSize = BaseApp.isLowEndHardware(context) ? VOLLEY_POOL_SIZE_SMALL : VOLLEY_POOL_SIZE;
        RequestQueue queue = new RequestQueue(
                new DiskBasedCache(CacheUtil.getCacheDir(context, VOLLEY_CACHE_DIR), VOLLEY_CACHE_SIZE),
                new BasicNetwork(new HurlStack()),
                poolSize,
                new SchedulerResponseDelivery()
        );
        queue.start();
        return queue;
    }

    @Provides @ArtworkFetcherScope
    public UriMatcher provideUriMatcher(@Named("artworkauthority") String authority) {
        return ArtworkUris.makeMatcher(authority);
    }

    @Provides @ArtworkFetcherScope @Named("oScheduler")
    public Scheduler provideObserveOnScheduler(@Named("fetcher") HandlerThread ht) {
        return AndroidSchedulers.handlerThread(new Handler(ht.getLooper()));
    }

    @Provides @ArtworkFetcherScope @Named("fetcher")
    public HandlerThread provideFetcherHandlerThread() {
        HandlerThread ht = new HandlerThread("Fetcher");
        ht.start();
        return ht;
    }

}
