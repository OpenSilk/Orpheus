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

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.utils.NavUtils;
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
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
import timber.log.Timber;

/**
 * Created by drew on 10/3/14.
 */
@WithModule(AlbumsScreen.Module.class)
public class AlbumsScreen extends Screen {

    @dagger.Module (
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
    public static class Presenter extends BasePresenter<LocalAlbum> {

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                         RxLoader<LocalAlbum> loader, OverflowHandlers.LocalAlbums popupHandler) {
            super(preferences, artworkRequestor, loader, popupHandler);
            Timber.v("new Albums.Presenter()");
        }

        @Override
        protected void load() {
            loader.setSortOrder(preferences.getString(AppPreferences.ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z));
            subscription = loader.getListObservable().subscribe(new Action1<List<LocalAlbum>>() {
                @Override
                public void call(List<LocalAlbum> localAlbums) {
                    if (viewNotNull()) {
                        getAdapter().addAll(localAlbums);
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
        protected void onItemClicked(View view, LocalAlbum item) {
            NavUtils.openAlbumProfile(view.getContext(), item);
        }

        @Override
        protected BaseAdapter<LocalAlbum> newAdapter() {
            return new Adapter(this, artworkRequestor);
        }

        @Override
        protected boolean isGrid() {
            return preferences.getString(AppPreferences.ALBUM_LAYOUT, AppPreferences.GRID).equals(AppPreferences.GRID);
        }

        @Override
        public ActionBarOwner.MenuConfig getMenuConfig() {
            ensureMenu();
            return actionBarMenu;
        }

        void setNewSortOrder(String sortOrder) {
            preferences.putString(AppPreferences.ALBUM_SORT_ORDER, sortOrder);
            reload();
        }

        void ensureMenu() {
            if (actionBarMenu == null) {
                Func1<Integer, Boolean> actionHandler = new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        switch (integer) {
                            case R.id.menu_sort_by_az:
                                setNewSortOrder(SortOrder.AlbumSortOrder.ALBUM_A_Z);
                                return true;
                            case R.id.menu_sort_by_za:
                                setNewSortOrder(SortOrder.AlbumSortOrder.ALBUM_Z_A);
                                return true;
                            case R.id.menu_sort_by_artist:
                                setNewSortOrder(SortOrder.AlbumSortOrder.ALBUM_ARTIST);
                                return true;
                            case R.id.menu_sort_by_year:
                                setNewSortOrder(SortOrder.AlbumSortOrder.ALBUM_YEAR);
                                return true;
                            case R.id.menu_sort_by_number_of_songs:
                                setNewSortOrder(SortOrder.AlbumSortOrder.ALBUM_NUMBER_OF_SONGS);
                                return true;
                            case R.id.menu_view_as_simple:
                                preferences.putString(AppPreferences.ALBUM_LAYOUT, AppPreferences.SIMPLE);
                                resetRecyclerView();
                                return true;
                            case R.id.menu_view_as_grid:
                                preferences.putString(AppPreferences.ALBUM_LAYOUT, AppPreferences.GRID);
                                resetRecyclerView();
                                return true;
                            default:
                                return false;
                        }
                    }
                };
                actionBarMenu = new ActionBarOwner.MenuConfig(actionHandler, R.menu.album_sort_by, R.menu.view_as);
            }
        }
    }

    static class Adapter extends BaseAdapter<LocalAlbum> {

        Adapter(BasePresenter<LocalAlbum> presenter, ArtworkRequestManager artworkRequestor) {
            super(presenter, artworkRequestor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, LocalAlbum album) {
            ArtInfo artInfo = new ArtInfo(album.artistName, album.name, album.artworkUri);
            holder.title.setText(album.name);
            holder.subtitle.setText(album.artistName);
            holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork, artInfo, ArtworkType.THUMBNAIL));
        }

    }



}
