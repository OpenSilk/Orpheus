/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.loader;

import android.content.Context;
import android.database.Cursor;

import com.andrew.apollo.model.LocalSong;

import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;
import org.opensilk.common.dagger.qualifier.ForApplication;

import javax.inject.Inject;

/**
 * Created by drew on 10/24/14.
 */
public class LocalSongsLoader extends RxCursorLoader<LocalSong> {
    @Inject
    public LocalSongsLoader(@ForApplication Context context) {
        super(context);
        setUri(Uris.EXTERNAL_MEDIASTORE_MEDIA);
        setProjection(Projections.LOCAL_SONG);
        setSelection(Selections.LOCAL_SONG);
        setSelectionArgs(SelectionArgs.LOCAL_SONG);
        //must set sort order
    }

    @Override
    protected LocalSong makeFromCursor(Cursor c) {
        return CursorHelpers.makeLocalSongFromCursor(null, c);
    }
}
