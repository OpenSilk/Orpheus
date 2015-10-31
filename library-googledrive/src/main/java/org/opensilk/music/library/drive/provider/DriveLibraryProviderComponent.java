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

package org.opensilk.music.library.drive.provider;

import org.opensilk.music.library.drive.DriveLibraryComponent;
import org.opensilk.music.library.drive.client.DriveClientComponent;
import org.opensilk.music.library.drive.client.DriveClientModule;

import dagger.Component;
import rx.functions.Func2;

/**
 * Created by drew on 10/20/15.
 */
@DriveLibraryProviderScope
@Component(
        dependencies = {
                DriveLibraryComponent.class,
        },
        modules = {
                DriveLibraryProviderModule.class
        }
)
public interface DriveLibraryProviderComponent {
        Func2<DriveLibraryComponent, DriveLibraryProviderModule, DriveLibraryProviderComponent> FACTORY =
                new Func2<DriveLibraryComponent, DriveLibraryProviderModule, DriveLibraryProviderComponent>() {
                        @Override
                        public DriveLibraryProviderComponent call(DriveLibraryComponent driveComponent, DriveLibraryProviderModule driveLibraryModule) {
                                return DaggerDriveLibraryProviderComponent.builder()
                                        .driveLibraryComponent(driveComponent)
                                        .driveLibraryProviderModule(driveLibraryModule)
                                        .build();
                        }
                };
        void inject(DriveLibraryProvider provider);
        DriveClientComponent driveClientComponent(DriveClientModule module);
}
