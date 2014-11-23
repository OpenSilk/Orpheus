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

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalSongGroup;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.music.R;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;

import javax.inject.Inject;

import rx.Observable;

/**
 * Created by drew on 11/19/14.
 */
public class LocalGenresProfileLoader extends RxCursorLoader<Object> {

    final Genre genre;

    @Inject
    public LocalGenresProfileLoader(@ForApplication Context context, Genre genre) {
        super(context);
        this.genre = genre;
        setUri(Uris.EXTERNAL_MEDIASTORE_ALBUMS);
        setProjection(Projections.LOCAL_ALBUM);
        setSelection(Selections.LOCAL_ALBUM + " AND " + Selections.LOCAL_ALBUMS(genre.mAlbumIds));
        setSelectionArgs(SelectionArgs.LOCAL_ALBUM);
        //must set sort order
    }

    @Override
    protected Object makeFromCursor(Cursor c) {
        return CursorHelpers.makeLocalAlbumFromCursor(c);
    }

    @Override
    public Observable<Object> createObservable() {
        return super.createObservable().startWith(
                new LocalSongGroup(context.getString(R.string.title_all_songs),
                        genre.mGenreName, genre.mSongIds, genre.mAlbumIds)
        );
    }
}
