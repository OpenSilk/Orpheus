/*
 * Copyright (C) 2012 Andrew Neal
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

package org.opensilk.music;

import android.app.Application;
import android.content.Context;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.provider.RecentStore;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.PreferenceUtils;
import com.andrew.apollo.utils.ThemeHelper;

import org.opensilk.cast.manager.MediaCastManager;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.artwork.ArtworkServiceImpl;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.ui.activities.HomeSlidingActivity;
import org.opensilk.silkdagger.DaggerApplication;
import org.opensilk.silkdagger.DaggerInjector;

import dagger.ObjectGraph;
import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Use to initilaze singletons and global static variables that require context
 */
public class MusicApp extends Application implements DaggerInjector {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    /**
     * Maximum size for artwork, this will be smallest screen width
     */
    public static int sDefaultMaxImageWidthPx;

    /**
     * Largest size of any thumbnail displayed
     */
    public static int DEFAULT_THUMBNAIL_SIZE_DP = 200;

    /**
     * Largest size a thumbnail will be
     */
    public static int sDefaultThumbnailWidthPx;

    /**
     * Contains the object graph, we use a singleton instance
     * to obtain the graph so we can inject our countent providers
     * which will be created before onCreate() is called.
     */
    private GraphHolder mGraphHolder;

    @Override
    @DebugLog
    public void onCreate() {
        super.onCreate();

        mGraphHolder = GraphHolder.get(this);

        if (DEBUG) {
            // Plant the forest
            Timber.plant(new Timber.DebugTree());
        }

        /*
         * Init global static variables
         */
        sDefaultMaxImageWidthPx = getMinDisplayWidth(getApplicationContext());
        sDefaultThumbnailWidthPx = convertDpToPx(getApplicationContext(), DEFAULT_THUMBNAIL_SIZE_DP);

        /*
         * XXXX Note to future drew. DO NOT INIT SINGLETONS HERE. They will be created twice!
         */

        /*
         * Debugging
         */
        // Enable strict mode logging
        enableStrictMode();
    }

    @Override
    public void inject(Object o) {
        mGraphHolder.inject(o);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return mGraphHolder.getObjectGraph();
    }

    private void enableStrictMode() {
        if (DEBUG) {
            final StrictMode.ThreadPolicy.Builder threadPolicyBuilder
                    = new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen();
            StrictMode.setThreadPolicy(threadPolicyBuilder.build());

            final StrictMode.VmPolicy.Builder vmPolicyBuilder
                    = new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .setClassInstanceLimit(MusicUtils.class, 1)
                    .setClassInstanceLimit(RecentStore.class, 1)
                    .setClassInstanceLimit(PreferenceUtils.class, 1)
                    .setClassInstanceLimit(ThemeHelper.class, 1)
                    .setClassInstanceLimit(MediaCastManager.class, 1)
                    .setClassInstanceLimit(BitmapDiskLruCache.class, 1)
                    .setClassInstanceLimit(BitmapDiskLruCache.class, 1)
                    .setClassInstanceLimit(ArtworkManager.class, 1)
                    .setClassInstanceLimit(HomeSlidingActivity.class, 1)
                    .setClassInstanceLimit(ArtworkServiceImpl.class, 1);
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

    /**
     * Converts given dp value to density specific pixel value
     * @param context
     * @param dp
     * @return
     */
    public static int convertDpToPx(Context context, float dp) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return Math.round(dp * (metrics.densityDpi / 160f));
    }

    /**
     * Returns smallest screen dimension
     * @param context
     * @return
     */
    public static int getMinDisplayWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        Point size = new Point();
//        wm.getDefaultDisplay().getSize(size);
//        return Math.min(size.x, size.y);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return Math.min(metrics.widthPixels, metrics.heightPixels);
    }
}
