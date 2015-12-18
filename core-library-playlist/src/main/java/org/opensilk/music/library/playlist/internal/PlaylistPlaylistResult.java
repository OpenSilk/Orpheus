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

package org.opensilk.music.library.playlist.internal;

import android.os.ResultReceiver;

import org.opensilk.music.library.playlist.PlaylistExtras;
import org.opensilk.music.library.playlist.PlaylistOperationListener;
import org.opensilk.music.model.Playlist;

/**
 * Created by drew on 12/17/15.
 */
public class PlaylistPlaylistResult implements PlaylistOperationListener<Playlist> {

    final ResultReceiver resultReceiver;

    public PlaylistPlaylistResult(ResultReceiver resultReceiver) {
        this.resultReceiver = resultReceiver;
    }

    @Override
    public void onError(String reason) {
        resultReceiver.send(PlaylistExtras.RESULT_ERROR, PlaylistExtras.b().putError(reason).get());
    }

    @Override
    public void onSuccess(Playlist val) {
        resultReceiver.send(PlaylistExtras.RESULT_SUCCESS, PlaylistExtras.b().putPlaylist(val).get());
    }
}
