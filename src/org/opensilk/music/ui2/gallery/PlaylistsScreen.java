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

import android.view.View;

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.common.widget.LetterTileDrawable;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;
import org.opensilk.music.ui2.profile.PlaylistScreen;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import mortar.ViewPresenter;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by drew on 10/19/14.
 */
@Layout(R.layout.gallery_page)
@WithModule(PlaylistsScreen.Module.class)
@GalleryPageTitle(R.string.page_playlists)
public class PlaylistsScreen extends Screen {

    @dagger.Module(
            addsTo = GalleryScreen.Module.class,
            injects = GalleryPageView.class
    )
    public static class Module {

        @Provides @Singleton
        public ViewPresenter<GalleryPageView> provideGalleryPagePresenter(Presenter presenter) {
            return presenter;
        }

    }

    @Singleton
    public static class Presenter extends BasePresenter<Playlist> {

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                         RxLoader<Playlist> loader, OverflowHandlers.Playlists popupHandler) {
            super(preferences, artworkRequestor, loader, popupHandler);
        }

        @Override
        protected void load() {
            subscription = loader.getObservable().subscribe(new SimpleObserver<Playlist>() {
                @Override
                public void onNext(Playlist playlist) {
                    if (viewNotNull()) {
                        getAdapter().addItem(playlist);
                        showRecyclerView();
                    }
                }
                @Override
                public void onCompleted() {
                    if (viewNotNull() && getAdapter().isEmpty()) showEmptyView();
                }
            });
        }

        @Override
        protected void onItemClicked(BaseAdapter.ViewHolder holder, Playlist item) {
            AppFlow.get(holder.itemView.getContext()).goTo(new PlaylistScreen(item));
        }

        @Override
        protected BaseAdapter<Playlist> newAdapter() {
            return new Adapter(this, artworkRequestor);
        }

        @Override
        protected boolean isGrid() {
            return preferences.getString(AppPreferences.PLAYLIST_LAYOUT, AppPreferences.GRID).equals(AppPreferences.GRID);
        }

        @Override
        public ActionBarOwner.MenuConfig getMenuConfig() {
            return new ActionBarOwner.MenuConfig.Builder()
                    .withMenus(R.menu.view_as)
                    .setActionHandler(new Func1<Integer, Boolean>() {
                        @Override
                        public Boolean call(Integer integer) {
                            switch (integer) {
                                case R.id.menu_view_as_simple:
                                    preferences.putString(AppPreferences.PLAYLIST_LAYOUT, AppPreferences.SIMPLE);
                                    resetRecyclerView();
                                    return true;
                                case R.id.menu_view_as_grid:
                                    preferences.putString(AppPreferences.PLAYLIST_LAYOUT, AppPreferences.GRID);
                                    resetRecyclerView();
                                    return true;
                                default:
                                    return false;
                            }
                        }
                    })
                    .build();
        }

    }

    static class Adapter extends BaseAdapter<Playlist> {

        Adapter(BasePresenter<Playlist> presenter, ArtworkRequestManager artworkRequestor) {
            super(presenter, artworkRequestor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, Playlist playlist) {
            holder.title.setText(playlist.mPlaylistName);
            holder.subtitle.setText(MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, playlist.mSongNumber));
            if (mGridStyle) {
                loadMultiArtwork(artworkRequestor,
                        holder.subscriptions,
                        playlist.mAlbumIds,
                        holder.artwork,
                        holder.artwork2,
                        holder.artwork3,
                        holder.artwork4
                );
            } else {
                LetterTileDrawable drawable = new LetterTileDrawable(holder.itemView.getResources());
                drawable.setText(playlist.mPlaylistName);
                holder.artwork.setImageDrawable(drawable);
            }
        }

        @Override
        protected boolean multiArtwork(int position) {
            return getItem(position).mAlbumIds.length >= 2;
        }
    }
}
