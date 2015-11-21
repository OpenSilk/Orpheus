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

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.app.BaseApp;
import org.opensilk.common.core.app.SimpleComponentCallbacks;
import org.opensilk.music.playback.appwidget.AppWidgetService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import mortar.MortarScope;
import timber.log.Timber;

public class App extends BaseApp {
    private static final boolean DEBUG = BuildConfig.DEBUG;

    @Override
    public void onCreate() {
        super.onCreate();
        setupTimber(DEBUG, null);
        registerComponentCallbacks(mComponentCallbacks);
        enableStrictMode();
    }

    @Override
    protected void onBuildRootScope(MortarScope.Builder builder) {
        if (isServiceProcess() || isEmulator()) {
            builder.withService(AppWidgetService.SERVICE_NAME, new AppWidgetService());
        }
    }

    @Override
    protected Object getRootComponent() {
        if (isUiProcess()) {
            return AppComponent.FACTORY.call(this);
        } else if (isProviderProcess()) {
            return ProviderComponent.FACTORY.call(this);
        } else if (isServiceProcess()) {
            return ServiceComponent.FACTORY.call(this);
        } else {
            Timber.e("Unable to determine our process");
            return EmulatorComponent.FACTORY.call(this);
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

    boolean isEmulator() {
        return StringUtils.isEmpty(getProcName());
    }

    private String mProcName;

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
                Timber.e(e, "getProcName");
                mProcName = "";
            }
        }
        return mProcName;
    }

    final ComponentCallbacks2 mComponentCallbacks = new SimpleComponentCallbacks() {
        @Override
        public void onTrimMemory(int level) {
            Timber.i("Trim memory level=%s for process %s", level, getProcName());
        }
    };

}
