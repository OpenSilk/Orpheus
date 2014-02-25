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

package org.opensilk.music.loaders;

import android.content.Context;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;

/**
 * Created by drew on 2/24/14.
 */
public class PlaylistSongCursorLoader extends CursorLoader {

    public PlaylistSongCursorLoader(Context context, long playlistId) {
        super(context);
        setUri(MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId));
        setProjection(Projections.PLAYLIST_SONGS);
        setSelection(new StringBuilder()
                .append(MediaStore.Audio.AudioColumns.IS_MUSIC + "=1")
                .append(" AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''").toString());
        setSelectionArgs(null);
        setSortOrder(MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER);
    }

}
