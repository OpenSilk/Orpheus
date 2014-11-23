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
import android.view.View;

import com.andrew.apollo.model.Genre;
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
import org.opensilk.music.ui2.profile.GenreScreen;
import org.opensilk.music.util.SortOrder;

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
@WithModule(GenresScreen.Module.class)
@GalleryPageTitle(R.string.page_genres)
public class GenresScreen extends Screen {

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
    public static class Presenter extends BasePresenter<Genre> {

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                         RxLoader<Genre> loader, OverflowHandlers.Genres popupHandler) {
            super(preferences, artworkRequestor, loader, popupHandler);
        }

        @Override
        protected void load() {
            loader.setSortOrder(preferences.getString(AppPreferences.GENRE_SORT_ORDER, SortOrder.GenreSortOrder.GENRE_A_Z));
            subscription = loader.getObservable().subscribe(new SimpleObserver<Genre>() {
                @Override
                public void onNext(Genre genre) {
                    if (viewNotNull()) {
                        getAdapter().addItem(genre);
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
        protected void onItemClicked(BaseAdapter.ViewHolder holder, Genre item) {
            AppFlow.get(holder.itemView.getContext()).goTo(new GenreScreen(item));
        }

        @Override
        protected BaseAdapter<Genre> newAdapter() {
            return new Adapter(this, artworkRequestor);
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

    static class Adapter extends BaseAdapter<Genre> {

        Adapter(BasePresenter<Genre> presenter, ArtworkRequestManager artworkRequestor) {
            super(presenter, artworkRequestor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, Genre genre) {
            holder.title.setText(genre.mGenreName);
            Context context = holder.itemView.getContext();
            String l2 = MusicUtils.makeLabel(context, R.plurals.Nalbums, genre.mAlbumNumber)
                    + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, genre.mSongNumber);
            holder.subtitle.setText(l2);
            if (mGridStyle) {
                loadMultiArtwork(artworkRequestor,
                        holder.subscriptions,
                        genre.mAlbumIds,
                        holder.artwork,
                        holder.artwork2,
                        holder.artwork3,
                        holder.artwork4
                );
            } else {
                LetterTileDrawable drawable = new LetterTileDrawable(holder.itemView.getResources());
                drawable.setText(genre.mGenreName);
                holder.artwork.setImageDrawable(drawable);
            }
        }

        @Override
        protected boolean multiArtwork(int position) {
            return getItem(position).mAlbumIds.length >= 2;
        }

    }
}
