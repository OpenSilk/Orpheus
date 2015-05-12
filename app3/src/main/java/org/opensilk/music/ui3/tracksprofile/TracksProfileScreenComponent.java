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

package org.opensilk.music.ui3.tracksprofile;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.profile.ProfileComponent;

import dagger.Component;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Created by drew on 5/12/15.
 */
@ScreenScope
@Component(
        dependencies = MusicActivityComponent.class,
        modules = TracksProfileScreenModule.class
)
public interface TracksProfileScreenComponent extends ProfileComponent {
    Func2<MusicActivityComponent, TracksProfileScreen, TracksProfileScreenComponent> FACTORY =
            new Func2<MusicActivityComponent, TracksProfileScreen, TracksProfileScreenComponent>() {
                @Override
                public TracksProfileScreenComponent call(MusicActivityComponent musicActivityComponent, TracksProfileScreen tracksProfileScreen) {
                    return DaggerTracksProfileScreenComponent.builder()
                            .musicActivityComponent(musicActivityComponent)
                            .tracksProfileScreenModule(new TracksProfileScreenModule(tracksProfileScreen))
                            .build();
                }
            };
}
