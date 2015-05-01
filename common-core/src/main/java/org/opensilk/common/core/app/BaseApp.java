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

package org.opensilk.common.core.app;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.StrictMode;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.core.util.VersionUtils;

import mortar.MortarScope;
import rx.functions.Action0;
import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 4/30/15.
 */
public abstract class BaseApp extends Application {

    protected MortarScope mRootScope;

    @Override
    public Object getSystemService(String name) {
        if (mRootScope == null) {
            MortarScope.Builder builder = MortarScope.buildRootScope()
                    .withService(DaggerService.DAGGER_SERVICE, getRootComponent());
            onBuildRootScope(builder);
            mRootScope = builder.build("ROOT");
        }
        if (mRootScope.hasService(name)) {
            return mRootScope.getService(name);
        }
        return super.getSystemService(name);
    }

    /**
     * @return Root Dagger Component for this process
     */
    protected abstract Object getRootComponent();

    /**
     * Add any additional services here
     * @param builder
     */
    protected void onBuildRootScope(MortarScope.Builder builder) {

    }

    protected void setupTimber(boolean debug, Action1<Throwable> silentExceptionHandler) {
        if (debug) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new ReleaseTree(silentExceptionHandler));
        }
    }

    protected void enableStrictMode() {
        final StrictMode.ThreadPolicy.Builder threadPolicyBuilder
                = new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .penaltyFlashScreen();
        StrictMode.setThreadPolicy(threadPolicyBuilder.build());

        final StrictMode.VmPolicy.Builder vmPolicyBuilder
                = new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog();
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }

    @TargetApi(19)
    public static boolean isLowEndHardware(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        if (VersionUtils.hasKitkat()) {
            return am.isLowRamDevice();
        } else if (VersionUtils.hasJellyBean()) {
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            return mi.totalMem < (512 * 1024 * 1024);
        } else {
            return Runtime.getRuntime().availableProcessors() == 1;
        }
    }

    /**
     * Stubs out v, d, and i logs, the optional Action1 allows sending execption to crash
     * reporter server or whatever.
     */
    private static final class ReleaseTree extends Timber.DebugTree {
        private final Action1<Throwable> silentExceptionHandler;

        public ReleaseTree(Action1<Throwable> exceptionHandler) {
            this.silentExceptionHandler = exceptionHandler;
        }

        //Tree stumps
        @Override public void v(String message, Object... args) {}
        @Override public void v(Throwable t, String message, Object... args) {}
        @Override public void d(String message, Object... args) {}
        @Override public void d(Throwable t, String message, Object... args) {}
        @Override public void i(String message, Object... args) {}
        @Override public void i(Throwable t, String message, Object... args) {}

        @Override public void e(Throwable t, String message, Object... args) {
            super.e(t, message, args);
            if (silentExceptionHandler != null && t != null) {
                silentExceptionHandler.call(t);
            }
        }

    }
}
