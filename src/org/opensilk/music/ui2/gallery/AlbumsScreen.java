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

import com.andrew.apollo.model.LocalAlbum;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.AppPreferences;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui2.loader.MediaStoreLoader;
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
 * Created by drew on 10/3/14.
 */
@WithModule(AlbumsScreen.Module.class)
@WithRecyclerViewPresenter(AlbumsScreen.Presenter.class)
public class AlbumsScreen extends Screen {

    @dagger.Module (
            addsTo = GalleryScreen.Module.class,
            injects = Presenter.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends BasePresenter {

        final Loader loader;

        @Inject
        public Presenter(AppPreferences preferences, Loader loader) {
            super(preferences);
            this.loader = loader;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad(%s)", savedInstanceState);
            super.onLoad(savedInstanceState);
            subscription = loader.getObservable().subscribe(new Action1<List<LocalAlbum>>() {
                @Override
                public void call(List<LocalAlbum> localAlbums) {
                    setAdapter(new Adapter(localAlbums));
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
    public static class Loader extends MediaStoreLoader<LocalAlbum> {

        @Inject
        public Loader(@ForApplication Context context) {
            super(context);
            setUri(Uris.EXTERNAL_MEDIASTORE_ALBUMS);
            setProjection(Projections.LOCAL_ALBUM);
            setSelection(Selections.LOCAL_ALBUM);
            setSelectionArgs(SelectionArgs.LOCAL_ALBUM);
            // need set sortorder
        }

        @Override
        protected LocalAlbum makeFromCursor(Cursor c) {
            return CursorHelpers.makeLocalAlbumFromCursor(c);
        }

    }

    static class Adapter extends BaseAdapter<LocalAlbum> {

        Adapter(List<LocalAlbum> items) {
            super(items);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            LocalAlbum album = items.get(position);
            holder.title.setText(album.name);
            holder.subtitle.setText(album.artistName);
            ArtworkManager.loadImage(new ArtInfo(album.artistName, album.name, album.artworkUri), holder.artwork);
        }

    }

}
