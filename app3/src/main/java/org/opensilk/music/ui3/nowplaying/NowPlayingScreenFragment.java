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

package org.opensilk.music.ui3.nowplaying;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.common.ui.mortarfragment.MortarFragment;
import org.opensilk.music.AppPreferences;

/**
 * Created by drew on 5/9/15.
 */
public class NowPlayingScreenFragment extends MortarFragment {
    @Override
    protected Screen newScreen() {
        NowPlayingActivityComponent component = DaggerService.getDaggerComponent(getActivity());
        AppPreferences settings = component.appPreferences();
        switch (settings.getString(AppPreferences.NOW_PLAYING_LAYOUT, AppPreferences.NOW_PLAYING_DEFAULT)) {
            case AppPreferences.NOW_PLAYING_CLASSIC:
                return new NowPlayingClassicScreen();
            case AppPreferences.NOW_PLAYING_DEFAULT:
            default:
                return new NowPlayingScreen();
        }
    }
}
