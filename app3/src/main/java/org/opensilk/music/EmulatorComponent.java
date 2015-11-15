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

package org.opensilk.music;

import org.opensilk.music.artwork.provider.ArtworkModule;
import org.opensilk.music.artwork.requestor.ArtworkRequestorModule;
import org.opensilk.music.index.IndexModule;
import org.opensilk.music.index.IndexProviderAuthorityModule;
import org.opensilk.music.index.client.IndexClientModule;
import org.opensilk.music.library.mediastore.MediaStoreLibraryAuthorityModule;
import org.opensilk.music.playback.PlaybackComponent;
import org.opensilk.music.playback.PlaybackModule;

import javax.inject.Singleton;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 10/19/15.
 */
@Singleton
@Component(
        modules = {
                AppModule.class,
                ArtworkRequestorModule.class,
                MediaStoreLibraryAuthorityModule.class,
                IndexProviderAuthorityModule.class,
                IndexClientModule.class,
                PlaybackModule.class,
                ArtworkModule.class,
                IndexModule.class,
        }
)
public interface EmulatorComponent extends AppComponent, ProviderComponent, PlaybackComponent {
    Func1<App, EmulatorComponent> FACTORY = new Func1<App, EmulatorComponent>() {
        @Override
        public EmulatorComponent call(App app) {
            return DaggerEmulatorComponent.builder()
                    .appModule(new AppModule(app))
                    .build();
        }
    };
}
