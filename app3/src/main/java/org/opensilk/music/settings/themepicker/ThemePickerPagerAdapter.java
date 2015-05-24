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

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;

import org.opensilk.common.ui.mortar.MortarPagerAdapter;
import org.opensilk.music.theme.OrpheusTheme;

import java.util.List;

/**
 * Created by drew on 5/24/15.
 */
class ThemePickerPagerAdapter extends MortarPagerAdapter<ThemePickerPageScreen, View> {
    final boolean lightTHeme;

    ThemePickerPagerAdapter(Context context, List<ThemePickerPageScreen> screens, boolean lighTHeme) {
        super(context, screens);
        this.lightTHeme = lighTHeme;
    }

    @Override
    protected Context decorateContext(Context newChildContext, int position) {
        OrpheusTheme theme = getTheme(position);
        return new ContextThemeWrapper(newChildContext, lightTHeme ? theme.light : theme.dark);
    }

    OrpheusTheme getTheme(int position) {
        return screens.get(position).orpheusTheme;
    }
}
