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

package org.opensilk.music.plugin.drive.ui;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.music.plugin.drive.GlobalComponent;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 4/29/15.
 */
@ActivityScope
@Component(
        dependencies = GlobalComponent.class
)
public interface ActivityComponent {
    Func1<GlobalComponent, ActivityComponent> FACTORY = new Func1<GlobalComponent, ActivityComponent>() {
        @Override
        public ActivityComponent call(GlobalComponent globalComponent) {
            return DaggerActivityComponent.builder()
                    .globalComponent(globalComponent)
                    .build();
        }
    };
    void inject(AuthTestFragment fragment);
//    void inject(SettingsFragment fragment);
}
