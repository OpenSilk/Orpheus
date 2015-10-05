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

import android.content.UriMatcher;
import android.os.Handler;
import android.os.HandlerThread;

import org.opensilk.music.artwork.ArtworkUris;
import org.opensilk.music.artwork.Constants;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.android.schedulers.HandlerScheduler;

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
    public UriMatcher provideUriMatcher(@Named("artworkauthority") String authority) {
        return ArtworkUris.makeMatcher(authority);
    }

    @Provides @ArtworkFetcherScope @Named("ObserveOnScheduler")
    public Scheduler provideObserveOnScheduler() {
        return HandlerScheduler.from(new Handler(service.getHandlerThread().getLooper()));
    }

    @Provides @ArtworkFetcherScope @Named("SubscribeOnScheduler")
    public Scheduler provideSubscribeOnScheduler() {
        return Constants.ARTWORK_SCHEDULER;
    }

}
