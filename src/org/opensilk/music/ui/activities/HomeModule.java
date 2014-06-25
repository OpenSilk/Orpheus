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
        injects = {
                HomeSlidingActivity.class,
        },
        // Im kinda unsure about this, it seems to work ok,
        // but the build complains with out it since HomeFragment
        // needs the ActionBarController, which is provided
        // by the ActivityModule so we don't know about it
        // until runtime, i guess, maybe ill figure out how
        // it all works someday
        complete = false
)
public class HomeModule {

    private final HomeSlidingActivity activity;

    public HomeModule(HomeSlidingActivity activity) {
        this.activity = activity;
    }

    @Provides @Singleton @ForActivity
    public DrawerHelper provideDrawerHelper() {
        return activity;
    }

}
