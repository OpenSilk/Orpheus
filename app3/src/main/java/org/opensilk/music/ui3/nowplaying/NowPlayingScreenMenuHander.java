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

import android.content.Context;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.pheelicks.visualizer.renderer.CircleBarRenderer;
import com.pheelicks.visualizer.renderer.CircleRenderer;

import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;

import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_ARTWORK;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_CIRCLE;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_CIRCLE_BAR;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_LINES;

/**
 * Created by drew on 10/12/15.
 */
public class NowPlayingScreenMenuHander implements ActionBarMenuHandler {

    final NowPlayingScreenPresenter presenter;
    final AppPreferences preferences;

    public NowPlayingScreenMenuHander(NowPlayingScreenPresenter presenter) {
        this.presenter = presenter;
        this.preferences =presenter.settings;
    }

    @Override
    public boolean onBuildMenu(MenuInflater menuInflater, Menu menu) {
        menuInflater.inflate(R.menu.now_playing, menu);
        String visualizerType = preferences.getString(NOW_PLAYING_VIEW, "none");
        switch (visualizerType) {
            case NOW_PLAYING_VIEW_VIS_CIRCLE: {
                menu.findItem(R.id.menu_visualizer_circle_lines).setChecked(true);
                break;
            }
            case NOW_PLAYING_VIEW_VIS_CIRCLE_BAR: {
                menu.findItem(R.id.menu_visualizer_circle_bars).setChecked(true);
                break;
            }
            case NOW_PLAYING_VIEW_ARTWORK: {
                menu.findItem(R.id.menu_visualizer_off).setChecked(true);
                break;
            }
        }
        return true;
    }

    @Override
    public boolean onMenuItemClicked(Context context, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_visualizer_circle_lines:
                preferences.putString(NOW_PLAYING_VIEW, NOW_PLAYING_VIEW_VIS_CIRCLE);
//                presenter.pokeVisRenderer();//TODO
                return true;
            case R.id.menu_visualizer_circle_bars:
                preferences.putString(NOW_PLAYING_VIEW, NOW_PLAYING_VIEW_VIS_CIRCLE_BAR);
//                presenter.pokeVisRenderer();
                return true;
            case R.id.menu_visualizer_off:
                preferences.putString(NOW_PLAYING_VIEW, NOW_PLAYING_VIEW_ARTWORK);
//                presenter.disableVisualizer();
                return true;
            default:
                return false;
        }
    }
}
