/*
 * Copyright (c) 2016 OpenSilk Productions LLC
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

package org.opensilk.music.ui3.library;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.ui3.MusicActivityComponent;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 1/4/16.
 */
@ScreenScope
@Component(
        dependencies = MusicActivityComponent.class,
        modules = LibraryOpScreenModule.class
)
public interface LibraryOpScreenComponent {
        Func2<MusicActivityComponent, LibraryOpScreen, LibraryOpScreenComponent> FACTORY =
                new Func2<MusicActivityComponent, LibraryOpScreen, LibraryOpScreenComponent>() {
                    @Override
                    public LibraryOpScreenComponent call(MusicActivityComponent musicActivityComponent, LibraryOpScreen libraryOpScreen) {
                        return DaggerLibraryOpScreenComponent.builder()
                                .musicActivityComponent(musicActivityComponent)
                                .libraryOpScreenModule(new LibraryOpScreenModule(libraryOpScreen))
                                .build();
                    }
                };
    void inject(LibraryOpScreenFragment fragment);
}
