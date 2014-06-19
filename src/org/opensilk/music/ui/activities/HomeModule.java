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

package org.opensilk.music.ui.activities;

import android.content.Context;

import com.andrew.apollo.ApolloModule;

import org.opensilk.music.ui.home.HomeAlbumFragment;
import org.opensilk.music.ui.home.HomeArtistFragment;
import org.opensilk.music.ui.home.HomeFragment;
import org.opensilk.music.ui.home.HomeRecentFragment;
import org.opensilk.music.ui.home.HomeSongFragment;
import org.opensilk.music.ui.library.LibraryHomeFragment;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.ActionBarControllerImpl;
import org.opensilk.music.ui.modules.DrawerHelper;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 6/16/14.
 */
@Module (
        library = true,
        addsTo = ApolloModule.class,
        injects = {
                HomeSlidingActivity.class,
                ActionBarControllerImpl.class,
                HomeAlbumFragment.class,
                HomeArtistFragment.class,
                HomeFragment.class,
                HomeRecentFragment.class,
                HomeSongFragment.class,
                LibraryHomeFragment.class,
        }
)
public class HomeModule {

    private final HomeSlidingActivity activity;

    public HomeModule(HomeSlidingActivity activity) {
        this.activity = activity;
    }

    @Provides @Singleton @ForActivity
    public HomeSlidingActivity provideActivity() {
        return activity;
    }

    @Provides @Singleton @ForActivity
    public Context provideActivityContext() {
        return activity;
    }

    @Provides @Singleton @ForActivity
    public DrawerHelper provideDrawerHelper() {
        return activity;
    }

    @Provides @Singleton @ForActivity
    public ActionBarController provideActionBarHelper(ActionBarControllerImpl controller) {
        return controller;
    }

}
