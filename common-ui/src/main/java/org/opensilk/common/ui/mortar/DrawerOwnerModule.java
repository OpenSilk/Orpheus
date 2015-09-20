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

package org.opensilk.common.ui.mortar;

import org.opensilk.common.core.dagger2.ActivityScope;

import dagger.Module;
import dagger.Provides;

/**
 * Activities may wish to hide their {@link DrawerOwner} from presenters. In that case
 * include this module with the activity module and declare methods for
 * {@link DrawerListenerRegistrar} and {@link DrawerController} in the component
 *
 * Created by drew on 9/19/15.
 */
@Module
public class DrawerOwnerModule {
    @Provides @ActivityScope
    public DrawerListenerRegistrar provideDrawerListenerRegistrar(DrawerOwner owner) {
        return owner;
    }
    @Provides @ActivityScope
    public DrawerController provideDrawerController(DrawerOwner owner) {
        return owner;
    }
}
