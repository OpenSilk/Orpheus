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
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;

import com.squareup.otto.Bus;

import org.opensilk.music.AppModule;
import org.opensilk.music.ui.folder.FolderPickerActivity;
import org.opensilk.music.ui.fragments.NowPlayingFragment;
import org.opensilk.music.ui.modules.ActionBarController;
import org.opensilk.music.ui.modules.ActionBarControllerImpl;
import org.opensilk.music.ui.settings.SettingsActivity;
import org.opensilk.silkdagger.qualifier.ForActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 6/20/14.
 */
@Module (
        injects = {
                ActionBarControllerImpl.class,
                HomeSlidingActivity.class,
                ProfileDialogActivity.class,
                NowPlayingFragment.class,
                SettingsActivity.class,
                FolderPickerActivity.class,
        },
        addsTo = AppModule.class,
        library = true
)
public class ActivityModule {

    private final ActionBarActivity activity;

    public ActivityModule (ActionBarActivity activity) {
        this.activity = activity;
    }

    @Provides @Singleton @ForActivity
    public ActionBarActivity provideActivity() {
        return activity;
    }

    @Provides @Singleton @ForActivity
    public FragmentActivity provideFragmentActivity() {
        return activity;
    }

    @Provides @Singleton @ForActivity
    public Context provideActivityContext() {
        return activity;
    }

    @Provides @Singleton @ForActivity
    public ActionBarController provideActionBarHelper(ActionBarControllerImpl controller) {
        return controller;
    }

    @Provides @Singleton @ForActivity
    public Bus provideActivityBus() {
        return new Bus("activity");
    }

}
