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

package org.opensilk.music.library.drive;

import android.content.Context;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.dagger2.AppContextModule;
import org.opensilk.common.core.dagger2.SystemServicesComponent;
import org.opensilk.music.okhttp.OkHttpComponent;

import javax.inject.Singleton;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 10/31/15.
 */
@Singleton
@Component(
        modules = {
                AppContextModule.class,
                DriveLibraryModule.class
        }
)
public interface DriveLibraryComponent extends OkHttpComponent, AppContextComponent, SystemServicesComponent {
    Func1<Context, DriveLibraryComponent> FACTORY = new Func1<Context, DriveLibraryComponent>() {
        @Override
        public DriveLibraryComponent call(Context context) {
            return DaggerDriveLibraryComponent.builder()
                    .appContextModule(new AppContextModule(context))
                    .build();
        }
    };
}
