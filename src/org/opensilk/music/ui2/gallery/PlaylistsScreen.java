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

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.provider.MusicProvider;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.DistinctAlbumArtInfoLoader;
import org.opensilk.music.ui2.loader.RxCursorLoader;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.functions.Action1;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(PlaylistsScreen.Module.class)
@WithRecyclerViewPresenter(PlaylistsScreen.Presenter.class)
public class PlaylistsScreen extends Screen {

    @dagger.Module(
            addsTo = GalleryScreen.Module.class,
            injects = Presenter.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends BasePresenter<Playlist> {

        final Loader loader;

        @Inject
        public Presenter(AppPreferences preferences, Loader loader) {
            super(preferences);
            this.loader = loader;
        }

        @Override
        protected BaseAdapter<Playlist> newAdapter(List<Playlist> items) {
            return new Adapter(items);
        }

        @Override
        protected void load() {
            subscription = loader.getListObservable().subscribe(new Action1<List<Playlist>>() {
                @Override
                public void call(List<Playlist> playlists) {
                    addItems(playlists);
                }
            });
        }

        @Override
        protected boolean isStaggered() {
            return true;
        }

        @Override
        public ActionBarOwner.MenuConfig getMenuConfig() {
            return null;
        }

    }

    @Singleton
    public static class Loader extends RxCursorLoader<Playlist> {

        @Inject
        public Loader(@ForApplication Context context) {
            super(context);
            setUri(MusicProvider.PLAYLIST_URI);
            // Dont need anything else
        }

        @Override
        protected Playlist makeFromCursor(Cursor c) {
            return CursorHelpers.makePlaylistFromCursor(c);
        }
    }

    static class Adapter extends BaseAdapter<Playlist> {

        Adapter(List<Playlist> items) {
            super(items);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Playlist playlist = getItem(position);
            holder.title.setText(playlist.mPlaylistName);
            holder.subtitle.setText(MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, playlist.mSongNumber));
            if (playlist.mAlbumNumber > 0) {
                DistinctAlbumArtInfoLoader loader = new DistinctAlbumArtInfoLoader(holder.itemView.getContext(), playlist.mAlbumIds);
                holder.subscriptions.add(loader.getDistinctObservable()
                        // only need at most 4
                        .take(4)
                        .subscribe(new Action1<ArtInfo>() {
                            @Override
                            public void call(ArtInfo artInfo) {
                                holder.loadArtwork(artInfo);
                            }
                        }));
            }
        }

        @Override
        protected boolean quadArtwork(int position) {
            return getItem(position).mAlbumNumber >= 4;
        }

        @Override
        protected boolean dualArtwork(int position) {
            return getItem(position).mAlbumNumber >= 2;
        }
    }
}
