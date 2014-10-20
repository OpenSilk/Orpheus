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

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import com.andrew.apollo.model.LocalSong;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.ui2.loader.DistinctAlbumArtInfoLoader;
import org.opensilk.music.ui2.loader.RxCursorLoader;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(SongsScreen.Module.class)
@WithRecyclerViewPresenter(SongsScreen.Presenter.class)
public class SongsScreen extends Screen {

    @dagger.Module(
            addsTo = GalleryScreen.Module.class,
            injects = Presenter.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends BasePresenter {

        Loader loader;

        @Inject
        public Presenter(AppPreferences preferences, Loader loader) {
            super(preferences);
            this.loader = loader;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad()");
            super.onLoad(savedInstanceState);
            subscription = loader.getListObservable().subscribe(new Action1<List<LocalSong>>() {
                @Override
                public void call(List<LocalSong> localSongs) {
                    setAdapter(new Adapter(localSongs));
                }
            });
        }

        @Override
        protected boolean isGrid() {
            return false;
        }

        @Override
        protected boolean isStaggered() {
            return false;
        }

    }

    @Singleton
    public static class Loader extends RxCursorLoader<LocalSong> {

        @Inject
        public Loader(@ForApplication Context context) {
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

    static class Adapter extends BaseAdapter<LocalSong> {

        Adapter(List<LocalSong> items) {
            super(items);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            LocalSong song = items.get(position);
            holder.title.setText(song.name);
            holder.subtitle.setText(song.artistName);
            // workaruond for mediastore to get the album artist
            DistinctAlbumArtInfoLoader loader = new DistinctAlbumArtInfoLoader(holder.itemView.getContext(), new long[]{song.albumId});
            holder.subscriptions.add(loader.getDistinctObservable().take(1).subscribe(new Action1<ArtInfo>() {
                @Override
                public void call(ArtInfo artInfo) {
                    holder.loadArtwork(artInfo);
                }
            }));
        }
    }
}
