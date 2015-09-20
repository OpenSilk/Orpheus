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

package org.opensilk.music.ui3.main;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.MusicActivityComponent;

import dagger.Component;
import rx.functions.Func1;
import rx.functions.Func2;

/**
 * Created by drew on 5/6/15.
 */
@ScreenScope
@Component(
        dependencies = MusicActivityComponent.class
)
public interface FooterScreenComponent {
    Func1<MusicActivityComponent, FooterScreenComponent> FACTORY =
            new Func1<MusicActivityComponent, FooterScreenComponent>() {
                @Override
                public FooterScreenComponent call(MusicActivityComponent musicActivityComponent) {
                    return DaggerFooterScreenComponent.builder()
                            .musicActivityComponent(musicActivityComponent)
                            .build();
                }
            };
    void inject(FooterScreenView view);
    AppPreferences appPreferences();
    PlaybackController playbackController();
    ArtworkRequestManager artworkRequestManager();
    DrawerOwner drawerOwner();
}
