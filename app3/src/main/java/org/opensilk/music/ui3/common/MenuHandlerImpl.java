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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;

import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.TrackList;
import org.opensilk.music.playback.control.PlaybackController;

/**
 * Created by drew on 9/24/15.
 */
public class MenuHandlerImpl {

    final PlaybackController playbackController;
    final AppPreferences appPreferences;
    final FragmentManagerOwner fm;

    public MenuHandlerImpl(
            PlaybackController playbackController,
            AppPreferences appPreferences,
            FragmentManagerOwner fm
    ) {
        this.playbackController = playbackController;
        this.appPreferences = appPreferences;
        this.fm = fm;
    }

    public void onBuildMenu(Model item, MenuInflater inflater, Menu menu) {
        if (item instanceof Album) {

        } else if (item instanceof Artist) {

        } else if (item instanceof Genre) {

        } else if (item instanceof TrackList) {

        } else if (item instanceof Track) {

        } else if (item instanceof Folder) {

        } else if (item instanceof Playlist) {

        }
    }

    public boolean onMenuItemClick(Context context, MenuAction action, Model item) {
        if (item instanceof Album) {

        } else if (item instanceof Artist) {

        } else if (item instanceof Genre) {

        } else if (item instanceof TrackList) {

        } else if (item instanceof Track) {

        } else if (item instanceof Folder) {

        } else if (item instanceof Playlist) {

        }
        return true;
    }

}
