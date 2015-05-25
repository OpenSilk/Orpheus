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

import android.content.res.Resources;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ComponentFactory;
import org.opensilk.common.ui.mortar.Layout;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortar.WithComponentFactory;
import org.opensilk.music.R;
import org.opensilk.music.ui.theme.OrpheusTheme;

import mortar.MortarScope;

/**
 * Created by drew on 5/24/15.
 */
@Layout(R.layout.settings_theme_picker_page)
@WithComponentFactory(ThemePickerPageScreen.Factory.class)
public class ThemePickerPageScreen extends Screen {
    OrpheusTheme orpheusTheme;

    ThemePickerPageScreen(OrpheusTheme orpheusTheme) {
        this.orpheusTheme = orpheusTheme;
    }

    @Override
    public String getName() {
        return super.getName() + orpheusTheme.toString();
    }

    public static class Factory extends ComponentFactory<ThemePickerPageScreen> {
        @Override
        protected Object createDaggerComponent(Resources resources, MortarScope parentScope, ThemePickerPageScreen screen) {
            return DaggerService.getDaggerComponent(parentScope);
        }
    }

}
