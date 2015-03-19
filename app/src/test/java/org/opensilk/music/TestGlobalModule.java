/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.opensilk.music.artwork.TestArtworkModule;
import org.opensilk.common.dagger.qualifier.ForApplication;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        library = true,
        includes = {
                TestArtworkModule.class,
        },
        injects = AppPreferences.class
)
public class TestGlobalModule {
    private Context appContext;

    public TestGlobalModule(Context context) {
        this.appContext = context.getApplicationContext();
        if (this.appContext == null) {
            throw new NullPointerException("Null application context what?");
        }
    }

    @Provides @Singleton @ForApplication
    public Context provideAppContext() {
        return appContext;
    }

    @Provides @Singleton
    public Gson provideGson() {
        return new GsonBuilder().create();
    }

}
