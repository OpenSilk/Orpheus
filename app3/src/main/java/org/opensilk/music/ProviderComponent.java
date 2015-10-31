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

import android.content.Context;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.dagger2.AppContextModule;
import org.opensilk.music.artwork.provider.ArtworkComponent;
import org.opensilk.music.artwork.provider.ArtworkModule;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.IndexModule;
import org.opensilk.music.lastfm.LastFMModule;
import org.opensilk.music.library.drive.DriveLibraryComponent;
import org.opensilk.music.library.drive.DriveLibraryModule;
import org.opensilk.music.library.upnp.UpnpLibraryAuthorityModule;
import org.opensilk.music.library.upnp.UpnpLibraryComponent;
import org.opensilk.music.library.upnp.UpnpLibraryModule;

import javax.inject.Singleton;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 5/1/15.
 */
@Singleton
@Component(
        modules = {
                AppContextModule.class,
                ArtworkModule.class,
                IndexModule.class,
                UpnpLibraryModule.class,
                DriveLibraryModule.class
        }
)
public interface ProviderComponent extends AppContextComponent,
        ArtworkComponent, IndexComponent, UpnpLibraryComponent, DriveLibraryComponent {
        Func1<Context, ProviderComponent> FACTORY = new Func1<Context, ProviderComponent>() {
                @Override
                public ProviderComponent call(Context context) {
                        return DaggerProviderComponent.builder()
                                .appContextModule(new AppContextModule(context))
                                .build();
                }
        };
}
