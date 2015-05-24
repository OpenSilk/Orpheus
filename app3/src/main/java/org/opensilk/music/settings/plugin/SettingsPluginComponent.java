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

package org.opensilk.music.settings.plugin;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.settings.SettingsActivityComponent;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 5/24/15.
 */
@ScreenScope
@Component(
        dependencies = SettingsActivityComponent.class
)
public interface SettingsPluginComponent {
    Func1<SettingsActivityComponent, SettingsPluginComponent> FACTORY =
            new Func1<SettingsActivityComponent, SettingsPluginComponent>() {
                @Override
                public SettingsPluginComponent call(SettingsActivityComponent settingsActivityComponent) {
                    return DaggerSettingsPluginComponent.builder()
                            .settingsActivityComponent(settingsActivityComponent)
                            .build();
                }
            };
    void inject(SettingsPluginRecyclerView view);
}
