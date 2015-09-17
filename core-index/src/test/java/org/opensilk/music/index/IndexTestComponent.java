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

package org.opensilk.music.index;

import android.content.Context;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.dagger2.AppContextModule;
import org.opensilk.music.index.database.IndexDatabase;
import org.opensilk.music.lastfm.LastFMComponent;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 9/16/15.
 */
@Singleton
@Component(
        modules = {
                AppContextModule.class,
                IndexTestModule.class
        }
)
public interface IndexTestComponent extends IndexComponent {
    Func1<Context, IndexTestComponent> FACTORY = new Func1<Context, IndexTestComponent>() {
        @Override
        public IndexTestComponent call(Context context) {
            return DaggerIndexTestComponent.builder()
                    .appContextModule(new AppContextModule(context))
                    .build();
        }
    };
}
