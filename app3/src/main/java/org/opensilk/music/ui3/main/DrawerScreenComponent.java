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

package org.opensilk.music.ui3.main;

import android.content.res.Resources;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.music.ui3.LauncherActivityComponent;

import dagger.Component;
import mortar.MortarScope;
import rx.functions.Func1;

/**
 * Created by drew on 5/1/15.
 */
@ScreenScope
@Component(
        dependencies = LauncherActivityComponent.class
)
public interface DrawerScreenComponent {
    Func1<LauncherActivityComponent, DrawerScreenComponent> FACTORY =
            new Func1<LauncherActivityComponent, DrawerScreenComponent>() {
                @Override
                public DrawerScreenComponent call(LauncherActivityComponent launcherActivityComponent) {
                    return DaggerDrawerScreenComponent.builder()
                            .launcherActivityComponent(launcherActivityComponent)
                            .build();
                }
            };
    void inject(DrawerScreenView view);
}
