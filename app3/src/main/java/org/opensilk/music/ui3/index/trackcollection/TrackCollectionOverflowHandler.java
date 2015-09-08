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

package org.opensilk.music.ui3.index.trackcollection;

import android.content.Context;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.TrackCollection;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by drew on 5/12/15.
 */
@ScreenScope
public class TrackCollectionOverflowHandler implements OverflowClickListener {

    public static final int[] MENUS = new int[] {
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_add_to_queue,
//                                R.menu.popup_add_to_playlist,
//                                R.menu.popup_more_by_artist,
//                                R.menu.popup_delete,
    };


    final PlaybackController playbackController;
    final AppPreferences appPreferences;
    final String sortOrderPref;

    @Inject
    public TrackCollectionOverflowHandler(
            PlaybackController playbackController,
            AppPreferences appPreferences,
            @Named("trackcollection_sortorderpref")String sortOrderPref
    ) {
        this.playbackController = playbackController;
        this.appPreferences = appPreferences;
        this.sortOrderPref = sortOrderPref;
    }

    @Override
    public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {
        for (int ii : MENUS) {
            m.inflate(ii);
        }
    }

    @Override
    public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {
        TrackCollection tc = (TrackCollection) item;
        String sortOrder = appPreferences.getString(appPreferences.makePrefKey(AppPreferences.KEY_INDEX,
                sortOrderPref), TrackSortOrder.A_Z);
        switch (action) {
            case PLAY_ALL:
                playbackController.playTracksFrom(tc.getTracksUri(), 0, sortOrder);
                return true;
            case SHUFFLE_ALL:
                playbackController.shuffleTracksFrom(tc.getTracksUri());
                return true;
            case ADD_TO_QUEUE:
                playbackController.addTracksToQueueFrom(tc.getTracksUri(), sortOrder);
                return true;
            case ADD_TO_PLAYLIST:
                //TODO
                return true;
            case MORE_BY_ARTIST:
                //TODO
                return true;
            case DELETE:
                //TODO
                return true;
            default:
                return false;
        }
    }
}
