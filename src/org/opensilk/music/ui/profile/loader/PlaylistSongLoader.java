/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui.profile.loader;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.ui2.loader.RxCursorLoader;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by drew on 2/24/14.
 */
public class PlaylistSongLoader extends RxCursorLoader<LocalSong> {

    @Inject
    public PlaylistSongLoader(@ForApplication Context context, @Named("playlist") long playlistId) {
        super(context);
        if (isLastAdded(playlistId)) {
            setUri(Uris.EXTERNAL_MEDIASTORE_MEDIA);
            setProjection(Projections.LOCAL_SONG);
            setSelection(Selections.LAST_ADDED);
            setSelectionArgs(SelectionArgs.LAST_ADDED());
            setSortOrder(SortOrder.LAST_ADDED);
        } else { //User generated playlist
            setUri(Uris.PLAYLIST(playlistId));
            setProjection(Projections.PLAYLIST_SONGS);
            setSelection(Selections.PLAYLIST_SONGS);
            setSelectionArgs(SelectionArgs.PLAYLIST_SONGS);
            setSortOrder(SortOrder.PLAYLIST_SONGS);
        }
    }

    @Override
    protected LocalSong makeFromCursor(Cursor c) {
        return CursorHelpers.makeLocalSongFromCursor(c);
    }

    private boolean isFavorites(long playlistId) {
        return playlistId == -1;
    }

    private boolean isLastAdded(long playlistId) {
        return playlistId == -2;
    }

}
