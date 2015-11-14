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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;

import static org.opensilk.music.AppPreferences.NOW_PLAYING_KEEP_SCREEN_ON;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_ARTWORK;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_CIRCLE;
import static org.opensilk.music.AppPreferences.NOW_PLAYING_VIEW_VIS_CIRCLE_BAR;

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
        boolean keepScreenOn = preferences.getBoolean(AppPreferences.NOW_PLAYING_KEEP_SCREEN_ON, false);
        if (keepScreenOn) {
            menu.findItem(R.id.menu_keep_screen_on).setChecked(true);
            menu.findItem(R.id.menu_keep_screen_on).setTitle(R.string.now_playing_keep_screen_on_checked);
        }
        String visualizerType = preferences.getString(NOW_PLAYING_VIEW, "none");
        switch (visualizerType) {
            case NOW_PLAYING_VIEW_VIS_CIRCLE: {
                menu.findItem(R.id.menu_visualizer_circle_lines).setChecked(true);
                menu.findItem(R.id.menu_visualizer_circle_lines)
                        .setTitle(R.string.now_playing_visualizer_circles_lines_checked);
                break;
            }
            case NOW_PLAYING_VIEW_VIS_CIRCLE_BAR: {
                menu.findItem(R.id.menu_visualizer_circle_bars).setChecked(true);
                menu.findItem(R.id.menu_visualizer_circle_bars)
                        .setTitle(R.string.now_playing_visualizer_circle_bars_checked);
                break;
            }
            case NOW_PLAYING_VIEW_ARTWORK:
            default: {
                menu.findItem(R.id.menu_visualizer_off).setChecked(true);
                menu.findItem(R.id.menu_visualizer_off)
                        .setTitle(R.string.now_playing_visualizer_off_checked);
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
                showToast(context);
//                presenter.pokeVisRenderer();
                return true;
            case R.id.menu_visualizer_circle_bars:
                preferences.putString(NOW_PLAYING_VIEW, NOW_PLAYING_VIEW_VIS_CIRCLE_BAR);
                showToast(context);
//                presenter.pokeVisRenderer();
                return true;
            case R.id.menu_visualizer_off:
                preferences.putString(NOW_PLAYING_VIEW, NOW_PLAYING_VIEW_ARTWORK);
                showToast(context);
//                presenter.disableVisualizer();
                return true;
            case R.id.menu_keep_screen_on:
                preferences.putBoolean(NOW_PLAYING_KEEP_SCREEN_ON, true);
                showToast(context);
                return true;
            default:
                return false;
        }
    }

    //TODO apply without forcing restart
    void showToast(Context context) {
        Toast.makeText(context, R.string.now_playing_close_reopen, Toast.LENGTH_LONG).show();
    }
}
