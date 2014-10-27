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

import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.utils.MusicUtils;
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
 * Created by drew on 10/19/14.
 */
@WithModule(ArtistsScreen.Module.class)
public class ArtistsScreen extends Screen {

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
    public static class Presenter extends BasePresenter<LocalArtist> {

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                         RxLoader<LocalArtist> loader, OverflowHandlers.LocalArtists popupHandler) {
            super(preferences, artworkRequestor, loader, popupHandler);
            Timber.v("new ArtistsScreen.Presenter()");
        }

        @Override
        protected void load() {
            loader.setSortOrder(preferences.getString(AppPreferences.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z));
            subscription = loader.getListObservable().subscribe(new Action1<List<LocalArtist>>() {
                @Override
                public void call(List<LocalArtist> localArtists) {
                    if (viewNotNull()) {
                        getAdapter().addAll(localArtists);
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
        protected void onItemClicked(View view, LocalArtist item) {
            NavUtils.openArtistProfile(view.getContext(), item);
        }

        @Override
        protected BaseAdapter<LocalArtist> newAdapter() {
            return new Adapter(this, artworkRequestor);
        }

        @Override
        protected boolean isGrid() {
            return preferences.getString(AppPreferences.ARTIST_LAYOUT, AppPreferences.GRID).equals(AppPreferences.GRID);
        }

        @Override
        public ActionBarOwner.MenuConfig getMenuConfig() {
            ensureMenu();
            return actionBarMenu;
        }

        void setNewSortOrder(String sortOrder) {
            preferences.putString(AppPreferences.ARTIST_SORT_ORDER, sortOrder);
            reload();
        }

        void ensureMenu() {
            if (actionBarMenu == null) {
                Func1<Integer, Boolean> actionHandler = new Func1<Integer, Boolean>() {
                    @Override
                    public Boolean call(Integer integer) {
                        switch (integer) {
                            case R.id.menu_sort_by_az:
                                setNewSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
                                return true;
                            case R.id.menu_sort_by_za:
                                setNewSortOrder(SortOrder.ArtistSortOrder.ARTIST_Z_A);
                                return true;
                            case R.id.menu_sort_by_number_of_songs:
                                setNewSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
                                return true;
                            case R.id.menu_sort_by_number_of_albums:
                                setNewSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_ALBUMS);
                                return true;
                            case R.id.menu_view_as_simple:
                                preferences.putString(AppPreferences.ARTIST_LAYOUT, AppPreferences.SIMPLE);
                                resetRecyclerView();
                                return true;
                            case R.id.menu_view_as_grid:
                                preferences.putString(AppPreferences.ARTIST_LAYOUT, AppPreferences.GRID);
                                resetRecyclerView();
                                return true;
                            default:
                                return false;
                        }
                    }
                };
                actionBarMenu = new ActionBarOwner.MenuConfig(actionHandler, R.menu.artist_sort_by, R.menu.view_as);
            }
        }
    }

    static class Adapter extends BaseAdapter<LocalArtist> {

        Adapter(BasePresenter<LocalArtist> presenter, ArtworkRequestManager artworkRequestor) {
            super(presenter, artworkRequestor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, LocalArtist artist) {
            ArtInfo artInfo = new ArtInfo(artist.name, null, null);
            holder.title.setText(artist.name);
            String subtitle = MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nalbums, artist.albumCount)
                + ", " + MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, artist.songCount);
            holder.subtitle.setText(subtitle);
            holder.subscriptions.add(artworkRequestor.newArtistRequest((AnimatedImageView)holder.artwork, artInfo, ArtworkType.THUMBNAIL));
        }
    }

}
