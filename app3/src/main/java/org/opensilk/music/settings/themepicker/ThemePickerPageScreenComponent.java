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

package org.opensilk.music.settings.themepicker;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.main.FooterScreenComponent;

import dagger.Component;
import rx.functions.Func1;

/**
 * Created by drew on 10/8/15.
 */
@ScreenScope
@Component(
        dependencies = MusicActivityComponent.class
)
public interface ThemePickerPageScreenComponent extends FooterScreenComponent {
    Func1<MusicActivityComponent, ThemePickerPageScreenComponent> FACTORY =
            new Func1<MusicActivityComponent, ThemePickerPageScreenComponent>() {
                @Override
                public ThemePickerPageScreenComponent call(MusicActivityComponent musicActivityComponent) {
                    return DaggerThemePickerPageScreenComponent.builder()
                            .musicActivityComponent(musicActivityComponent)
                            .build();
                }
            };
}
