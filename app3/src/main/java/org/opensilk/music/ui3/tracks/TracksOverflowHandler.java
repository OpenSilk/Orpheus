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

package org.opensilk.music.ui3.tracks;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;

import java.util.Collections;

import javax.inject.Inject;

import rx.functions.Func0;

/**
 * Created by drew on 5/9/15.
 */
@ScreenScope
public class TracksOverflowHandler implements OverflowClickListener {

    public static final int[] MENUS = new int[] {
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
//            R.menu.popup_add_to_playlist,
//            R.menu.popup_more_by_artist,
//            R.menu.popup_set_ringtone,
//            R.menu.popup_delete,
    };

    final LibraryConfig libraryConfig;
    final LibraryInfo libraryInfo;
    final PlaybackController playbackController;

    @Inject
    public TracksOverflowHandler(
            LibraryConfig libraryConfig,
            LibraryInfo libraryInfo,
            PlaybackController playbackController
    ) {
        this.libraryConfig = libraryConfig;
        this.libraryInfo = libraryInfo;
        this.playbackController = playbackController;
    }

    @Override
    public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {
        for (int ii : MENUS) {
            m.inflate(ii);
        }
    }

    @Override
    public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {
        final Uri uri = LibraryUris.track(libraryConfig.authority,
                libraryInfo.libraryId, item.getIdentity());
        switch (action) {
            case PLAY_NEXT:
                playbackController.enqueueAllNext(Collections.singletonList(uri));
                return true;
            case ADD_TO_QUEUE:
                playbackController.addAllToQueue(Collections.singletonList(uri));
                return true;
            case ADD_TO_PLAYLIST:
                //TODO
                return true;
            case MORE_BY_ARTIST:
                //TODO
                return true;
            case SET_RINGTONE:
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
