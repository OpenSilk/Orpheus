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

package org.opensilk.music.ui2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.otto.Bus;

import org.opensilk.music.AppModule;
import org.opensilk.music.ui2.core.android.AndroidModule;
import org.opensilk.music.ui2.core.lifecycle.LifecycleModule;
import org.opensilk.music.util.GsonParcer;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import flow.Parcer;
import rx.Observable;

/**
 * Created by drew on 10/6/14.
 */
@Module (
        injects = LauncherActivity.class,
        includes = {
                AndroidModule.class,
                LifecycleModule.class,
        },
        addsTo = AppModule.class,
        complete = false,
        library = true
)
public class ActivityModule {

    @Provides @Singleton @Named("activity")
    public Bus provideEventBus() {
        return new Bus("activity");
    }

    // Flow backstack

    @Provides @Singleton
    public Gson provideGson() {
        return new GsonBuilder().create();
    }

    @Provides @Singleton
    public Parcer<Object> provideParcer(Gson gson) {
        return new GsonParcer<Object>(gson);
    }

}
