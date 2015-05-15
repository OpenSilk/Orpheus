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

package org.opensilk.music.ui3.albums;

import android.content.Context;
import android.net.Uri;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;
import org.opensilk.music.ui3.delete.DeleteScreenFragment;

import javax.inject.Inject;

/**
 * Created by drew on 5/9/15.
 */
@ScreenScope
public class AlbumsOverflowHandler implements OverflowClickListener {

    public static final int[] MENUS = new int[]{
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_add_to_queue,
//                                R.menu.popup_add_to_playlist,
//                                R.menu.popup_more_by_artist,
            R.menu.popup_delete,
    };

    final LibraryConfig libraryConfig;
    final LibraryInfo libraryInfo;
    final PlaybackController playbackController;
    final AppPreferences appPreferences;
    final FragmentManagerOwner fm;

    @Inject
    public AlbumsOverflowHandler(
            LibraryConfig libraryConfig,
            LibraryInfo libraryInfo,
            PlaybackController playbackController,
            AppPreferences appPreferences,
            FragmentManagerOwner fm
    ) {
        this.libraryConfig = libraryConfig;
        this.libraryInfo = libraryInfo;
        this.playbackController = playbackController;
        this.appPreferences = appPreferences;
        this.fm =fm;
    }

    @Override
    public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {
        for (int ii : MENUS) {
            m.inflate(ii);
        }
    }

    @Override
    public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {
        Uri uri = LibraryUris.albumTracks(libraryConfig.authority,
                libraryInfo.libraryId, item.getIdentity());
        String sortOrder = appPreferences.getString(appPreferences.makePluginPrefKey(libraryConfig,
                AppPreferences.ALBUM_TRACK_SORT_ORDER), TrackSortOrder.PLAYORDER);
        switch (action) {
            case PLAY_ALL:
                playbackController.playTracksFrom(uri, 0, sortOrder);
                return true;
            case SHUFFLE_ALL:
                playbackController.shuffleTracksFrom(uri);
                return true;
            case ADD_TO_QUEUE:
                playbackController.addTracksToQueueFrom(uri, sortOrder);
                return true;
            case ADD_TO_PLAYLIST:
                //TODO
                return true;
            case MORE_BY_ARTIST:
                //TODO
                return true;
            case DELETE:
                DeleteScreenFragment f = DeleteScreenFragment.ni(context, libraryConfig, libraryInfo, item);
                fm.showDialog(f);
                return true;
            default:
                return false;
        }
    }
}
