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

package org.opensilk.music.ui3.library;

import android.content.res.Resources;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.MusicActivityToolbarComponent;

import mortar.MortarScope;

/**
 * Created by drew on 5/1/15.
 */
@Layout(R.layout.screen_library_landing)
@WithComponentFactory(LandingScreen.Factory.class)
public class LandingScreen extends Screen {
    final LibraryConfig libraryConfig;

    public LandingScreen(LibraryConfig libraryConfig) {
        this.libraryConfig = libraryConfig;
    }

    @Override
    public String getName() {
        return getClass().getName() + libraryConfig.authority;
    }

    public static class Factory extends ComponentFactory<LandingScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, LandingScreen screen) {
            MusicActivityToolbarComponent activityToolbarComponent = DaggerService.getDaggerComponent(parentScope);
            return LandingScreenComponent.FACTORY.call(activityToolbarComponent, screen);
        }
    }

}
