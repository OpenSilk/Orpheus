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

import org.opensilk.common.core.dagger2.SubScreenScope;

import dagger.Subcomponent;
import rx.functions.Func2;

/**
 * Created by drew on 9/17/15.
 */
@SubScreenScope
@Subcomponent(
        modules = FooterPageScreenModule.class
)
public interface FooterPageScreenComponent {
    Func2<FooterScreenComponent, FooterPageScreen, FooterPageScreenComponent> FACTORY =
            new Func2<FooterScreenComponent, FooterPageScreen, FooterPageScreenComponent>() {
                @Override
                public FooterPageScreenComponent call(FooterScreenComponent musicActivityComponent, FooterPageScreen footerPageScreen) {
                    return musicActivityComponent.footerPageScreenComponent(new FooterPageScreenModule(footerPageScreen));
                }
            };
    void inject(FooterPageScreenView view);
}
