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

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import mortar.ViewPresenter;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(SongsScreen.Module.class)
public class SongsScreen extends Screen {

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
    public static class Presenter extends BasePresenter<LocalSong> {

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                         RxLoader<LocalSong> loader, OverflowHandlers.LocalSongs popupHandler) {
            super(preferences, artworkRequestor, loader, popupHandler);
        }

        @Override
        protected void load() {
            loader.setSortOrder(preferences.getString(AppPreferences.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z));
            subscription = loader.getListObservable().subscribe(new Action1<List<LocalSong>>() {
                @Override
                public void call(List<LocalSong> localSongs) {
                    if (viewNotNull()) {
                        getAdapter().addAll(localSongs);
                        showRecyclerView();
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {

                }
            }, new Action0() {
                @Override
                public void call() {
                    if (viewNotNull() && getAdapter().isEmpty()) showEmptyView();
                }
            });
        }

        @Override
        protected void onItemClicked(BaseAdapter.ViewHolder holder, LocalSong item) {
            ((OverflowHandlers.LocalSongs) popupHandler).play(item);
        }

        @Override
        protected BaseAdapter<LocalSong> newAdapter() {
            return new Adapter(this, artworkRequestor);
        }

        @Override
        public ActionBarOwner.MenuConfig getMenuConfig() {
            ensureMenu();
            return actionBarMenu;
        }

        void setNewSortOrder(String sortOrder) {
            preferences.putString(AppPreferences.SONG_SORT_ORDER, sortOrder);
            reload();
        }

        void ensureMenu() {
            if (actionBarMenu == null) {
                Func1<Integer, Boolean> actionHandler = new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        switch (integer) {
                            case R.id.menu_sort_by_az:
                                setNewSortOrder(SortOrder.SongSortOrder.SONG_A_Z);
                                return true;
                            case R.id.menu_sort_by_za:
                                setNewSortOrder(SortOrder.SongSortOrder.SONG_Z_A);
                                return true;
                            case R.id.menu_sort_by_artist:
                                setNewSortOrder(SortOrder.SongSortOrder.SONG_ARTIST);
                                return true;
                            case R.id.menu_sort_by_album:
                                setNewSortOrder(SortOrder.SongSortOrder.SONG_ALBUM);
                                return true;
                            case R.id.menu_sort_by_year:
                                setNewSortOrder(SortOrder.SongSortOrder.SONG_YEAR);
                                return true;
                            case R.id.menu_sort_by_duration:
                                setNewSortOrder(SortOrder.SongSortOrder.SONG_DURATION);
                                return true;
                            case R.id.menu_sort_by_filename:
                                setNewSortOrder(SortOrder.SongSortOrder.SONG_FILENAME);
                                return true;
                            default:
                                return false;
                        }
                    }
                };
                actionBarMenu = new ActionBarOwner.MenuConfig(actionHandler, R.menu.song_sort_by);
            }
        }
    }

    static class Adapter extends BaseAdapter<LocalSong> {

        Adapter(BasePresenter<LocalSong> presenter, ArtworkRequestManager artworkRequestor) {
            super(presenter, artworkRequestor);
            setGridStyle(true);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, LocalSong song) {
            holder.title.setText(song.name);
            holder.subtitle.setText(song.artistName);
            holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork,
                    null, song.albumId, ArtworkType.THUMBNAIL));
        }
    }
}
