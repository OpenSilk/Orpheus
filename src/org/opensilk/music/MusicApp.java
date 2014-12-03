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

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.WindowManager;

import com.splunk.mint.Mint;

import org.apache.commons.io.FileUtils;
import org.opensilk.cast.manager.MediaCastManager;
import org.opensilk.music.artwork.ArtworkRequestManagerImpl;
import org.opensilk.music.artwork.cache.ArtworkLruCache;
import org.opensilk.music.artwork.cache.BitmapDiskLruCache;
import org.opensilk.music.ui2.LauncherActivity;
import org.opensilk.common.dagger.DaggerInjector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import dagger.ObjectGraph;
import hugo.weaving.DebugLog;
import mortar.Mortar;
import mortar.MortarScope;
import timber.log.Timber;

/**
 * Use to initilaze singletons and global static variables that require context
 */
public class MusicApp extends Application implements DaggerInjector {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    /** Largest size of artwork */
    public static final int MAX_ARTWORK_SIZE_DP = 720;
    /** Largest size of any thumbnail displayed */
    public static final int DEFAULT_THUMBNAIL_SIZE_DP = 200;
    /** Maximum size for artwork, this will be smallest screen width */
    public static int sDefaultMaxImageWidthPx;
    /** Largest size a thumbnail will be */
    public static int sDefaultThumbnailWidthPx;
    /** Disable some features depending on device type */
    public static boolean sIsLowEndHardware;

    /**
     * Contains the object graph, we use a singleton instance
     * to obtain the graph so we can inject our countent providers
     * which will be created before onCreate() is called.
     */
    private GraphHolder mGraphHolder;

    protected ObjectGraph mScopedGraphe;

    protected MortarScope mRootScope;

    @Inject AppPreferences mSettings;

    @Override
    @DebugLog
    public void onCreate() {
        super.onCreate();

        //logs
        Timber.plant(DEBUG ? new Timber.DebugTree() : new ReleaseTree());

        //prevents allocating resources the service doesnt need
        final boolean isMainProcess = !isServiceProcess();

        if (isMainProcess) {
            //graph setup
            setupDagger();
            setupMortar();
            inject(this);
        }

        // Init global static variables
        sDefaultMaxImageWidthPx = Math.min(
                getMinDisplayWidth(getApplicationContext()),
                convertDpToPx(getApplicationContext(), MAX_ARTWORK_SIZE_DP)
        );
        sDefaultThumbnailWidthPx = convertDpToPx(getApplicationContext(), DEFAULT_THUMBNAIL_SIZE_DP);
        sIsLowEndHardware = isLowEndHardware(getApplicationContext());

        /*
         * Debugging
         */

        // Enable strict mode logging
        enableStrictMode();

        // crash reports
        Mint.disableNetworkMonitoring();
        if (isMainProcess
                    && !TextUtils.isEmpty(BuildConfig.SPLUNK_MINT_KEY)
                    && mSettings.getBoolean(AppPreferences.SEND_CRASH_REPORTS, true)) {
                Mint.initAndStartSession(getApplicationContext(), BuildConfig.SPLUNK_MINT_KEY);
        }

    }

    protected void setupDagger() {
        mGraphHolder = GraphHolder.get(this);
        mScopedGraphe = mGraphHolder.getObjectGraph().plus(new AppModule(this));
    }

    protected void setupMortar() {
        mRootScope = Mortar.createRootScope(DEBUG, mScopedGraphe);
    }

    @Override
    public void inject(Object o) {
        mScopedGraphe.inject(o);
    }

    @Override
    public ObjectGraph getObjectGraph() {
        return mScopedGraphe;
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mRootScope;
        }
        return super.getSystemService(name);
    }

    protected void enableStrictMode() {
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
                    .setClassInstanceLimit(MediaCastManager.class, 1)
                    .setClassInstanceLimit(BitmapDiskLruCache.class, 1)
                    .setClassInstanceLimit(ArtworkLruCache.class, 1)
                    .setClassInstanceLimit(LauncherActivity.class, 1)
                    .setClassInstanceLimit(ArtworkRequestManagerImpl.class, 1);
            StrictMode.setVmPolicy(vmPolicyBuilder.build());
        }
    }

    /** Converts given dp value to density specific pixel value */
    public static int convertDpToPx(Context context, float dp) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics);
    }

    /** Returns smallest screen dimension */
    public static int getMinDisplayWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        Point size = new Point();
//        wm.getDefaultDisplay().getSize(size);
//        return Math.min(size.x, size.y);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        return Math.min(metrics.widthPixels, metrics.heightPixels);
    }

    public static boolean isLowEndHardware(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            return am.isLowRamDevice();
        } else {
            return Runtime.getRuntime().availableProcessors() == 1;
        }
    }

    @DebugLog
    boolean isServiceProcess() {
        try {
            final File comm = new File("/proc/self/comm");
            if (comm.exists() && comm.canRead()) {
                final List<String> commLines = FileUtils.readLines(comm);
                if (commLines.size() > 0) {
                    final String procName = commLines.get(0).trim();
                    Timber.i("%s >> %s ", comm.getAbsolutePath(), procName);
                    return procName.endsWith(":service");
                }
            }
        } catch (IOException ignored) { }
        return false;
    }

    private static class ReleaseTree extends Timber.DebugTree {
        //Tree stumps
        @Override public void v(String message, Object... args) {}
        @Override public void v(Throwable t, String message, Object... args) {}
        @Override public void d(String message, Object... args) {}
        @Override public void d(Throwable t, String message, Object... args) {}
        @Override public void i(String message, Object... args) {}
        @Override public void i(Throwable t, String message, Object... args) {}

        @Override public void e(Throwable t, String message, Object... args) {
            super.e(t, message, args);
            sendException(t);
        }

        static void sendException(Throwable t) {
            try {
                if (t instanceof Exception)
                    Mint.logException((Exception)t);
            } catch (Exception ignored) {/*safety*/}
        }

    }
}
