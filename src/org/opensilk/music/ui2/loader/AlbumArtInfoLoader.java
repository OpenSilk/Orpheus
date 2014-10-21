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
import android.provider.MediaStore;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/19/14.
 */
public class AlbumArtInfoLoader extends RxCursorLoader<ArtInfo> {

    public AlbumArtInfoLoader(Context context, long[] albumIds) {
        super(context);
        setUri(Uris.EXTERNAL_MEDIASTORE_ALBUMS);
        setProjection(Projections.LOCAL_ALBUM);
        setAlbumIds(albumIds);
        setSelectionArgs(SelectionArgs.LOCAL_ALBUM);
        setSortOrder(MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
    }

    public void setAlbumIds(long[] albumIds) {
        setSelection(Selections.LOCAL_ALBUM + " AND " + Selections.LOCAL_ALBUMS(albumIds));
    }

    public Observable<ArtInfo> getDistinctObservable() {
        return createObservable()
                .distinct()
                        // Im not really concered with errors right now
                        // just log it, and return an empty list
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Timber.e(throwable, "RxCursorLoader(uri=%s :: projection=%s :: selection=%s " +
                                "selectionArgs=%s :: sortOrder=%s", uri, projection, selection, selectionArgs, sortOrder);
                    }
                })
                .onExceptionResumeNext(Observable.<ArtInfo>empty())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    protected ArtInfo makeFromCursor(Cursor c) {
        return CursorHelpers.makeArtInfoFromLocalAlbumCursor(c);
    }

}
