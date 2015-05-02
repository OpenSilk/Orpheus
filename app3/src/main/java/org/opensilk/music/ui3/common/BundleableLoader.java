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

package org.opensilk.music.ui3.common;

import android.content.Context;
import android.content.UriMatcher;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.util.CursorUtil;
import org.opensilk.music.model.spi.Bundleable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 5/2/15.
 */
public class BundleableLoader extends RxCursorLoader<Bundleable> {

    final UriMatcher uriMatcher;

    public BundleableLoader(
            @ForApplication Context context,
            @Named("loader_uri") Uri uri,
            @Named("loader_proj")String[] projection,
            @Named("loader_sel")String selection,
            @Named("loader_selargsi")String[] selectionArgs,
            @Named("loader_sortorder")String sortOrder
    ) {
        super(context, uri, projection, selection, selectionArgs, sortOrder);
        uriMatcher = LibraryUris.makeMatcher(uri.getAuthority());
    }

    @Override
    protected Bundleable makeFromCursor(Cursor c) throws Exception {
        switch (uriMatcher.match(uri)) {
            case LibraryUris.M_ALBUMS:
            case LibraryUris.M_ALBUM:
                return CursorUtil.fromAlbumCursor(c);
            case LibraryUris.M_ARTISTS:
            case LibraryUris.M_ARTIST:
                return CursorUtil.fromArtistCursor(c);
            case LibraryUris.M_FOLDERS:
            case LibraryUris.M_FOLDER:
                return CursorUtil.fromFolderTrackCursor(c);
            case LibraryUris.M_TRACKS:
            case LibraryUris.M_TRACK:
                    return CursorUtil.fromTrackCursor(c);
            default:
                throw new IllegalArgumentException("Uri not matched! " + uri);
        }
    }
}
