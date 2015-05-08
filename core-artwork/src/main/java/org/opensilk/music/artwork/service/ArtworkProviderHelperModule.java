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

package org.opensilk.music.artwork.service;

import android.content.Context;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.music.artwork.UtilsArt;
import org.opensilk.music.artwork.cache.BitmapLruCache;
import org.opensilk.music.artwork.shared.ArtworkAuthorityModule;
import org.opensilk.music.artwork.shared.GsonModule;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 5/6/15.
 */
@Module(
        includes = {
                ArtworkAuthorityModule.class,
                GsonModule.class
        }
)
public class ArtworkProviderHelperModule {
    @Provides @Singleton @Named("helpercache")
    public BitmapLruCache provideBitmapLruCache(@ForApplication Context context) {
        return new BitmapLruCache(UtilsArt.calculateL1CacheSize(context, false));
    }
}
