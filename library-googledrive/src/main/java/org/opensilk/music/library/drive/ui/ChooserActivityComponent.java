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

package org.opensilk.music.library.drive.ui;

import org.opensilk.common.core.dagger2.ActivityScope;
import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.music.library.drive.DriveAuthorityModule;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 10/21/15.
 */
@ActivityScope
@Component(
        dependencies = AppContextComponent.class,
        modules = DriveAuthorityModule.class
)
public interface ChooserActivityComponent {
    Func1<AppContextComponent, ChooserActivityComponent> FACTORY =
            new Func1<AppContextComponent, ChooserActivityComponent>() {
                @Override
                public ChooserActivityComponent call(AppContextComponent appContextComponent) {
                    return DaggerChooserActivityComponent.builder()
                            .appContextComponent(appContextComponent)
                            .build();
                }
            };
    void inject(ChooserActivity chooserActivity);
}
