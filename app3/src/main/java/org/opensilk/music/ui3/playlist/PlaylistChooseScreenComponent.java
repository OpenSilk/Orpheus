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

package org.opensilk.music.ui3.playlist;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.main.FooterScreenComponent;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 10/24/15.
 */
@ScreenScope
@Component(
        dependencies = MusicActivityComponent.class,
        modules = PlaylistChooseScreenModule.class
)
public interface PlaylistChooseScreenComponent extends BundleableComponent, FooterScreenComponent {
    Func2<MusicActivityComponent, PlaylistChooseScreenModule, PlaylistChooseScreenComponent> FACTORY =
            new Func2<MusicActivityComponent, PlaylistChooseScreenModule, PlaylistChooseScreenComponent>() {
                @Override
                public PlaylistChooseScreenComponent call(MusicActivityComponent musicActivityComponent,
                                                          PlaylistChooseScreenModule playlistChooseScreenModule) {
                    return DaggerPlaylistChooseScreenComponent.builder()
                            .musicActivityComponent(musicActivityComponent)
                            .playlistChooseScreenModule(playlistChooseScreenModule)
                            .build();
                }
            };
    void inject(PlaylistChooseScreenView view);
}
