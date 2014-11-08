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

import com.andrew.apollo.model.LocalArtist;

import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;
import org.opensilk.common.dagger.qualifier.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by drew on 10/24/14.
 */
@Singleton
public class LocalArtistsLoader extends RxCursorLoader<LocalArtist> {
    @Inject
    public LocalArtistsLoader(@ForApplication Context context) {
        super(context);
        setUri(Uris.EXTERNAL_MEDIASTORE_ARTISTS);
        setProjection(Projections.LOCAL_ARTIST);
        setSelection(Selections.LOCAL_ARTIST);
        setSelectionArgs(SelectionArgs.LOCAL_ARTIST);
        //must set sort order
    }

    @Override
    protected LocalArtist makeFromCursor(Cursor c) {
        return CursorHelpers.makeLocalArtistFromCursor(c);
    }
}
