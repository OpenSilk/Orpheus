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

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Environment;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.app.BaseApp;
import org.opensilk.common.core.app.SimpleComponentCallbacks;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.artwork.fetcher.ArtworkFetcherService;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.PlaybackComponent;
import org.opensilk.music.playback.service.PlaybackServiceComponent;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.functions.Action1;
import timber.log.Timber;

public class App extends BaseApp {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    //Only for UiProcess
    @Inject ArtworkRequestManager mArtworkRequestor;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        android.support.multidex.MultiDex.install(this);
    }

    @Override
    @DebugLog
    public void onCreate() {
        super.onCreate();

        setupTimber(DEBUG, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                //TODO
            }
        });

        if (isUiProcess()) {
            DaggerService.<AppComponent>getDaggerComponent(this).inject(this);
            registerComponentCallbacks(mUiComponentCallbacks);
        } else if (isProviderProcess()) {
            // STOPSHIP: 9/19/15
            File db = getDatabasePath("music.db");
            Timber.e("Database path = %s", db);
            try {
                FileUtils.copyFile(db, new File(Environment.getExternalStorageDirectory(), "orpheus.db"));
            } catch (Exception e) {

            }
        } else if (isServiceProcess()) {

        } else {
            throw new RuntimeException("Unable to determine our process");
        }

        // Enable strict mode logging (we do this after reading the process to avoid a warning)
        enableStrictMode();
    }

    @Override
    protected Object getRootComponent() {
        if (isUiProcess()) {
            return AppComponent.FACTORY.call(this);
        } else if (isProviderProcess()) {
            return ProviderComponent.FACTORY.call(this);
        } else if (isServiceProcess()) {
            return PlaybackComponent.FACTORY.call(this);
        } else {
            throw new RuntimeException("Unable to determine our process");
        }
    }

    boolean isServiceProcess() {
        return StringUtils.endsWith(getProcName(), ":service");
    }

    boolean isUiProcess() {
        return StringUtils.endsWith(getProcName(), ":ui");
    }

    boolean isProviderProcess() {
        return StringUtils.endsWith(getProcName(), ":prvdr");
    }

    private String mProcName;

    @DebugLog
    String getProcName() {
        if (mProcName == null) {
            try {
                final File comm = new File("/proc/self/comm");
                if (comm.exists() && comm.canRead()) {
                    final List<String> commLines = FileUtils.readLines(comm);
                    if (commLines.size() > 0) {
                        mProcName = StringUtils.trimToEmpty(commLines.get(0));
                        Timber.i("%s >> %s ", comm.getAbsolutePath(), mProcName);
                    }
                }
            } catch (IOException e) {
                Timber.e(e, "isServiceProcess");
                mProcName = "";
            }
        }
        return mProcName;
    }

    final ComponentCallbacks2 mUiComponentCallbacks = new SimpleComponentCallbacks() {
        @Override
        @DebugLog
        public void onTrimMemory(int level) {
            if (level >= TRIM_MEMORY_COMPLETE) {
                //mArtworkRequestor.onDeathImminent();
            } else if (level >= 15 /*TRIM_MEMORY_RUNNING_CRITICAL*/) {
                mArtworkRequestor.evictL1();
                Runtime.getRuntime().gc();
            }
        }
    };

}
