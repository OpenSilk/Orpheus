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

package org.opensilk.music.ui3.folders;

import android.content.Context;
import android.widget.PopupMenu;

import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.FolderTrackSortOrder;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;

import java.util.Collections;

import javax.inject.Inject;

/**
 * Created by drew on 5/9/15.
 */
public class FoldersOverflowHandler implements OverflowClickListener {

    public static final int[] MENUS_FOLDER = new int[] {
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
    };
    public static final int[] MENUS_SONG = new int[] {
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
    };

    final LibraryConfig libraryConfig;
    final LibraryInfo libraryInfo;
    final PlaybackController playbackController;
    final AppPreferences appPreferences;

    @Inject
    public FoldersOverflowHandler(
            LibraryConfig libraryConfig,
            LibraryInfo libraryInfo,
            PlaybackController playbackController,
            AppPreferences appPreferences
    ) {
        this.libraryConfig = libraryConfig;
        this.libraryInfo = libraryInfo;
        this.playbackController = playbackController;
        this.appPreferences = appPreferences;
    }

    @Override
    public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {
        if (item instanceof Folder) {
            for (int ii : MENUS_FOLDER) {
                m.inflate(ii);
            }
        } else if (item instanceof Track) {
            for (int ii : MENUS_SONG) {
                m.inflate(ii);
            }
        }
    }

    @Override
    public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {
        final String auth = libraryConfig.authority;
        final String lib = libraryInfo.libraryId;
        final String foldersSortOrder = appPreferences.getString(appPreferences.makePluginPrefKey(
                libraryConfig, AppPreferences.FOLDER_SORT_ORDER), FolderTrackSortOrder.A_Z);
        switch (action) {
            case PLAY_ALL: {
                if (item instanceof Folder) {
                    playbackController.playTracksFrom(
                            LibraryUris.folderTracks(auth, lib, item.getIdentity()),
                            0,
                            foldersSortOrder
                    );
                } else {
                    return false;
                }
                return true;
            }
            case SHUFFLE_ALL: {
                if (item instanceof Folder) {
                    playbackController.shuffleTracksFrom(
                            LibraryUris.folderTracks(auth, lib, item.getIdentity())
                    );
                } else {
                    return false;
                }
                return true;
            }
            case PLAY_NEXT: {
                if (item instanceof Track) {
                    playbackController.enqueueAllNext(Collections.singletonList(
                            LibraryUris.track(auth, lib, item.getIdentity())
                    ));
                } else if (item instanceof Folder) {
                    playbackController.enqueueTracksNextFrom(
                            LibraryUris.folderTracks(auth, lib, item.getIdentity()),
                            foldersSortOrder
                    );
                } else {
                    return false;
                }
                return true;
            }
            case ADD_TO_QUEUE: {
                if (item instanceof Track) {
                    playbackController.addAllToQueue(Collections.singletonList(
                            LibraryUris.track(auth, lib, item.getIdentity())
                    ));
                } else if (item instanceof Folder) {
                    playbackController.addTracksToQueueFrom(
                            LibraryUris.folderTracks(auth, lib, item.getIdentity()),
                            foldersSortOrder
                    );
                } else {
                    return false;
                }
                return true;
            }
            default:
                return false;
        }
    }
}
