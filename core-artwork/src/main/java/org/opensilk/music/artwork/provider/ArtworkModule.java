/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.artwork.provider;

import android.content.Context;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.artwork.cache.CacheUtil;
import org.opensilk.music.artwork.coverartarchive.CoverArtArchiveModule;
import org.opensilk.music.artwork.shared.ArtworkAuthorityModule;
import org.opensilk.music.artwork.shared.ArtworkPreferences;
import org.opensilk.music.artwork.shared.GsonModule;
import org.opensilk.music.lastfm.LastFMModule;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import static org.opensilk.music.artwork.Constants.DISK_CACHE_DIRECTORY;

/**
 * Created by drew on 6/21/14.
 */
@Module(
        includes = {
                ArtworkAuthorityModule.class,
                GsonModule.class,
                SystemServicesModule.class,
                LastFMModule.class,
                CoverArtArchiveModule.class
        }
)
public class ArtworkModule {

    @Provides @Singleton //TODO when/how to close this?
    public BitmapDiskCache provideBitmapDiskLruCache(@ForApplication Context context, ArtworkPreferences preferences) {
        final int size = Integer.decode(preferences.getString(ArtworkPreferences.IMAGE_DISK_CACHE_SIZE, "60")) * 1024 * 1024;
        return BitmapDiskLruCache.open(CacheUtil.getCacheDir(context, DISK_CACHE_DIRECTORY), size);
    }

    @Provides @Singleton @Named("artworkscheduler")
    public Scheduler provideArtworkScheduler() {
        return Schedulers.io();//TODO using computation for both provider and fetcher will
                               //create deadlock maybe use io for provider and computation for fetcher
    }
}
