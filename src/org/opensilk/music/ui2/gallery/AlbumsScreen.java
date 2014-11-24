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

import com.andrew.apollo.model.LocalAlbum;

import org.opensilk.music.util.SortOrder;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.PaletteObserver;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;
import org.opensilk.music.ui2.profile.AlbumScreen;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import flow.Layout;
import hugo.weaving.DebugLog;
import mortar.ViewPresenter;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 10/3/14.
 */
@Layout(R.layout.gallery_page)
@WithModule(AlbumsScreen.Module.class)
@GalleryPageTitle(R.string.page_albums)
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
        @DebugLog
        protected void load() {
            loader.setSortOrder(preferences.getString(AppPreferences.ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z));
            subscription = loader.getListObservable().subscribe(new SimpleObserver<List<LocalAlbum>>() {
                @Override
                @DebugLog
                public void onNext(List<LocalAlbum> localAlbums) {
                    if (viewNotNull()) {
                        getAdapter().addAll(localAlbums);
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
        protected void onItemClicked(BaseAdapter.ViewHolder holder, LocalAlbum item) {
            AppFlow.get(holder.itemView.getContext()).goTo(new AlbumScreen(item));
        }

        @Override
        protected BaseAdapter<LocalAlbum> newAdapter() {
            return new Adapter(this, artworkRequestor);
        }

        @Override
        protected boolean isGrid() {
            return preferences.getString(AppPreferences.ALBUM_LAYOUT, AppPreferences.GRID).equals(AppPreferences.GRID);
        }

        void setNewSortOrder(String sortOrder) {
            preferences.putString(AppPreferences.ALBUM_SORT_ORDER, sortOrder);
            reload();
        }

        @Override
        protected void ensureMenu() {
            if (actionBarMenu == null) {
                actionBarMenu = new ActionBarOwner.MenuConfig.Builder()
                        .withMenus(R.menu.album_sort_by, R.menu.view_as)
                        .setActionHandler(new Func1<Integer, Boolean>() {
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
                        })
                        .build();
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
            PaletteObserver paletteObserver = holder.descriptionContainer != null
                    ? holder.descriptionContainer.getPaletteObserver() : null;
            holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork,
                    paletteObserver, artInfo, ArtworkType.THUMBNAIL));
        }

    }



}
