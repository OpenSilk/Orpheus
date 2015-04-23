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

package org.opensilk.music;

import android.content.Context;
import android.net.ConnectivityManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.opensilk.common.dagger.AppContextModule;
import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.artwork.ArtworkModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 6/21/14.
 */
@Module(
        addsTo = AppContextModule.class,
        library = true,
        includes = {
                ArtworkModule.class,
        },
        injects = AppPreferences.class
)
public class GlobalModule {

    @Provides @Singleton
    public Gson provideGson() {
        return new GsonBuilder().create();
    }

    @Provides @Singleton
    public ConnectivityManager provideConnectivityManager(@ForApplication Context appContext) {
        return (ConnectivityManager)appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

}
