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

import com.andrew.apollo.utils.PreferenceUtils;

import org.opensilk.music.util.Projections;

/**
 * Created by drew on 2/21/14.
 */
public class AlbumSongCursorLoader extends CursorLoader {

    public AlbumSongCursorLoader(Context context, long albumId) {
        super(context);
        setUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
        setProjection(Projections.SONG);
        setSelection(new StringBuilder()
                .append(MediaStore.Audio.AudioColumns.IS_MUSIC + "=1")
                .append(" AND " + MediaStore.Audio.AudioColumns.TITLE + " != ''")
                .append(" AND " + MediaStore.Audio.AudioColumns.ALBUM_ID + "=" + albumId)
                .toString());
        setSelectionArgs(null);
        setSortOrder(PreferenceUtils.getInstance(context).getAlbumSongSortOrder());
    }

}
