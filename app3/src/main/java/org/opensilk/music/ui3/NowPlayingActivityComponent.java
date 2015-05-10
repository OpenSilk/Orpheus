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

package org.opensilk.music.ui3;

import android.content.Context;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.ui.mortar.ActionBarOwner;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortar.ActivityResultsOwnerModule;
import org.opensilk.common.ui.mortar.PauseAndResumeModule;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.music.AppComponent;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.main.MainPresenter;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 5/9/15.
 */
@ActivityScope
@Component(
        dependencies = AppComponent.class,
        modules = {
                PauseAndResumeModule.class,
        }
)
public interface NowPlayingActivityComponent {
    Func1<AppComponent, NowPlayingActivityComponent> FACTORY =
            new Func1<AppComponent, NowPlayingActivityComponent>() {
                @Override
                public NowPlayingActivityComponent call(AppComponent appComponent) {
                    return DaggerNowPlayingActivityComponent.builder()
                            .appComponent(appComponent)
                            .build();
                }
            };
    @ForApplication Context appContext();
    AppPreferences appPreferences();
    ArtworkRequestManager artworkRequestor();
    //ActionBarOwner actionBarOwner();
    PlaybackController playbackController();
    PauseAndResumeRegistrar pauseAndResumeRegistrar();
    void inject(NowPlayingActivity activty);
}
