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

package org.opensilk.music.ui2.loader;

import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.model.LocalSong;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by drew on 2/21/14.
 */
public class LocalAlbumSongLoader extends RxCursorLoader<LocalSong> {

    @Inject
    public LocalAlbumSongLoader(@ForApplication Context context, @Named("album") long albumId) {
        super(context);
        setUri(Uris.EXTERNAL_MEDIASTORE_MEDIA);
        setProjection(Projections.LOCAL_SONG);
        setSelection(Selections.LOCAL_ALBUM_SONGS);
        setSelectionArgs(SelectionArgs.LOCAL_ALBUM_SONGS(albumId));
        // need set sortorder
    }

    @Override
    protected LocalSong makeFromCursor(Cursor c) {
        return CursorHelpers.makeLocalSongFromCursor(c);
    }

}
