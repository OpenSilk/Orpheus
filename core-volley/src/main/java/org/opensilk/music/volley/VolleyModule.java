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

package org.opensilk.music.volley;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;

import org.opensilk.common.core.app.BaseApp;
import org.opensilk.common.core.dagger2.ForApplication;

import java.io.File;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

import static org.opensilk.music.volley.Constants.*;

/**
 * Created by drew on 9/1/15.
 */
@Module
public class VolleyModule {

    @Provides @Singleton
    public RequestQueue provideRequestQueue(@ForApplication Context context) {
        final int poolSize = BaseApp.isLowEndHardware(context) ? VOLLEY_POOL_SIZE_SMALL : VOLLEY_POOL_SIZE;
        RequestQueue queue = new RequestQueue(
                new DiskBasedCache(getCacheDir(context, VOLLEY_CACHE_DIR), VOLLEY_CACHE_SIZE),
                new BasicNetwork(new HurlStack()),
                poolSize,
                new SchedulerResponseDelivery()
        );
        queue.start();
        return queue;
    }

    /**
     * Get a usable cache directory (external if available, internal otherwise)
     *
     * @param context The {@link android.content.Context} to use
     * @param uniqueName A unique directory name to append to the cache
     *            directory
     * @return The cache directory
     */
    public static File getCacheDir(final Context context, final String uniqueName) {
        File cachePath = context.getExternalCacheDir();
        if (cachePath == null || !cachePath.canWrite()) {
            cachePath = context.getCacheDir();
        }
        File cacheDir = new File(cachePath, uniqueName);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        if (!cacheDir.canWrite()) {
            cacheDir.setWritable(true);
        }
        return cacheDir;
    }
}
