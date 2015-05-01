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

package org.opensilk.music.artwork.fetcher;

import org.opensilk.music.artwork.ArtworkComponent;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 5/1/15.
 */
@ArtworkFetcherScope
@Component(
        dependencies = ArtworkComponent.class,
        modules = ArtworkFetcherModule.class
)
public interface ArtworkFetcherComponent {
    Func2<ArtworkComponent, ArtworkFetcherService, ArtworkFetcherComponent> FACTORY =
            new Func2<ArtworkComponent, ArtworkFetcherService, ArtworkFetcherComponent>() {
                @Override
                public ArtworkFetcherComponent call(ArtworkComponent artworkComponent, ArtworkFetcherService artworkFetcherService) {
                    return DaggerArtworkFetcherComponent.builder()
                            .artworkComponent(artworkComponent)
                            .artworkFetcherModule(new ArtworkFetcherModule(artworkFetcherService))
                            .build();
                }
            };
    void inject(ArtworkFetcherService service);
}
