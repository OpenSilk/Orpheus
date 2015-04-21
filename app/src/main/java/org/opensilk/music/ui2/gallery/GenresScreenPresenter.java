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

package org.opensilk.music.ui2.gallery;

import com.andrew.apollo.model.Genre;

import org.opensilk.common.flow.AppFlow;
import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;
import org.opensilk.music.ui2.profile.GenreScreen;
import org.opensilk.music.util.SortOrder;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.functions.Func1;

/**
 * Created by drew on 4/20/15.
 */
@Singleton
public class GenresScreenPresenter extends GalleryPagePresenter<Genre> {

    @Inject
    public GenresScreenPresenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                                 RxLoader<Genre> loader, OverflowHandlers.Genres popupHandler) {
        super(preferences, artworkRequestor, loader, popupHandler);
    }

    @Override
    protected void load() {
        loader.setSortOrder(preferences.getString(AppPreferences.GENRE_SORT_ORDER, SortOrder.GenreSortOrder.GENRE_A_Z));
        subscription = loader.getObservable().subscribe(new SimpleObserver<Genre>() {
            @Override
            public void onNext(Genre genre) {
                addItem(genre);
            }

            @Override
            public void onCompleted() {
                if (viewNotNull() && getAdapter().isEmpty()) showEmptyView();
            }
        });
    }

    @Override
    protected void onItemClicked(GalleryPageAdapter.ViewHolder holder, Genre item) {
        AppFlow.get(holder.itemView.getContext()).goTo(new GenreScreen(item));
    }

    @Override
    protected GalleryPageAdapter<Genre> newAdapter() {
        return new GenresScreenAdapter(this, artworkRequestor);
    }

    @Override
    protected boolean isGrid() {
        return preferences.getString(AppPreferences.GENRE_LAYOUT, AppPreferences.GRID).equals(AppPreferences.GRID);
    }

    void setNewSortOrder(String sortOrder) {
        preferences.putString(AppPreferences.GENRE_SORT_ORDER, sortOrder);
        reload();
    }

    @Override
    protected void ensureMenu() {
        if (actionBarMenu == null) {
            actionBarMenu = new ActionBarOwner.MenuConfig.Builder()
                    .withMenus(R.menu.genre_sort_by, R.menu.view_as)
                    .setActionHandler(new Func1<Integer, Boolean>() {
                        @Override
                        public Boolean call(Integer integer) {
                            switch (integer) {
                                case R.id.menu_sort_by_az:
                                    setNewSortOrder(SortOrder.GenreSortOrder.GENRE_A_Z);
                                    return true;
                                case R.id.menu_sort_by_za:
                                    setNewSortOrder(SortOrder.GenreSortOrder.GENRE_Z_A);
                                    return true;
                                case R.id.menu_view_as_simple:
                                    preferences.putString(AppPreferences.GENRE_LAYOUT, AppPreferences.SIMPLE);
                                    resetRecyclerView();
                                    return true;
                                case R.id.menu_view_as_grid:
                                    preferences.putString(AppPreferences.GENRE_LAYOUT, AppPreferences.GRID);
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
