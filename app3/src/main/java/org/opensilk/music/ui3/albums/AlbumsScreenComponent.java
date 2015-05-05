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

package org.opensilk.music.ui3.albums;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.ui3.MusicActivity;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.folders.FoldersScreenModule;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 5/5/15.
 */
@ScreenScope
@Component(
        dependencies = MusicActivityComponent.class,
        modules = AlbumsScreenModule.class
)
public interface AlbumsScreenComponent extends BundleableComponent {
    Func2<MusicActivityComponent, AlbumsScreen, AlbumsScreenComponent> FACTORY =
            new Func2<MusicActivityComponent, AlbumsScreen, AlbumsScreenComponent>() {
                @Override
                public AlbumsScreenComponent call(MusicActivityComponent musicActivityComponent, AlbumsScreen albumsScreen) {
                    return DaggerAlbumsScreenComponent.builder()
                            .musicActivityComponent(musicActivityComponent)
                            .albumsScreenModule(new AlbumsScreenModule(albumsScreen))
                            .build();
                }
            };
}
