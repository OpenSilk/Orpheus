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

package org.opensilk.music.artwork.provider;

import android.content.Context;

import org.opensilk.common.core.dagger2.AppContextModule;
import org.opensilk.music.artwork.shared.ArtworkComponentCommon;
import org.opensilk.music.artwork.cache.BitmapDiskCache;
import org.opensilk.music.artwork.shared.GsonComponent;
import org.opensilk.music.lastfm.LastFMComponent;
import org.opensilk.music.volley.VolleyComponent;
import org.opensilk.music.volley.VolleyModule;

import javax.inject.Singleton;

import dagger.Component;
import de.umass.lastfm.LastFM;
import rx.functions.Func1;

/**
 * Root component for provider process, This isnt used directly but extended to allow
 * additional providers to be used. Annotations for documentation and clarity.
 *
 * Created by drew on 5/1/15.
 */
@Singleton
@Component(
        modules = {
                AppContextModule.class,
                ArtworkModule.class,
        }
)
public interface ArtworkComponent extends ArtworkComponentCommon, GsonComponent,
        SystemServicesComponent, VolleyComponent, LastFMComponent {
//    Func1<Context, ArtworkComponent> FACTORY = new Func1<Context, ArtworkComponent>() {
//        @Override
//        public ArtworkComponent call(Context context) {
//            return DaggerArtworkComponent.builder()
//                    .appContextModule(new AppContextModule(context))
//                    .build();
//        }
//    };
    void inject(ArtworkProvider provider);
    BitmapDiskCache bitmapDiskCache();
}
