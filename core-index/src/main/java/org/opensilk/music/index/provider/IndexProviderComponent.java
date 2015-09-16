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

package org.opensilk.music.index.provider;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.index.IndexProviderAuthorityModule;
import org.opensilk.music.index.database.IndexDatabase;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 7/11/15.
 */
@IndexProviderScope
@Component(
        dependencies = IndexComponent.class
)
public interface IndexProviderComponent {
    Func1<IndexComponent, IndexProviderComponent> FACTORY = new Func1<IndexComponent, IndexProviderComponent>() {
        @Override
        public IndexProviderComponent call(IndexComponent indexComponent) {
            return DaggerIndexProviderComponent.builder()
                    .indexComponent(indexComponent)
                    .build();
        }
    };
    void inject(IndexProvider provider);
}
