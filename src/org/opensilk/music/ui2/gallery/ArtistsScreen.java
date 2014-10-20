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

import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.SortOrder;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
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
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(ArtistsScreen.Module.class)
@WithRecyclerViewPresenter(ArtistsScreen.Presenter.class)
public class ArtistsScreen extends Screen {

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
            Timber.v("new ArtistsScreen.Presenter()");
            this.loader = loader;
            this.loader.setSortOrder(preferences.getString(AppPreferences.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z));
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad()");
            super.onLoad(savedInstanceState);
            reload();
        }

        void reload() {
            if (subscription != null) subscription.unsubscribe();
            subscription = loader.getListObservable().subscribe(new Action1<List<LocalArtist>>() {
                @Override
                public void call(List<LocalArtist> localArtists) {
                    setAdapter(new Adapter(localArtists));
                }
            });
        }

        void setNewSortOrder(String sortOrder) {
            preferences.putString(AppPreferences.ARTIST_SORT_ORDER, sortOrder);
            loader.setSortOrder(sortOrder);
            reload();
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
//                                NavUtils.goHome(getActivity());
                                return true;
                            case R.id.menu_view_as_grid:
                                preferences.putString(AppPreferences.ARTIST_LAYOUT, AppPreferences.GRID);
//                                NavUtils.goHome(getActivity());
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

    @Singleton
    public static class Loader extends RxCursorLoader<LocalArtist> {

        @Inject
        public Loader(@ForApplication Context context) {
            super(context);
            setUri(Uris.EXTERNAL_MEDIASTORE_ARTISTS);
            setProjection(Projections.LOCAL_ARTIST);
            setSelection(Selections.LOCAL_ARTIST);
            setSelectionArgs(SelectionArgs.LOCAL_ARTIST);
            //must set sort order
        }

        @Override
        protected LocalArtist makeFromCursor(Cursor c) {
            return CursorHelpers.makeLocalArtistFromCursor(c);
        }

    }

    static class Adapter extends BaseAdapter<LocalArtist> {

        Adapter(List<LocalArtist> items) {
            super(items);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            LocalArtist artist = getItem(position);
            holder.title.setText(artist.name);
            String subtitle = "";
            if (artist.albumCount > 0) {
                subtitle = MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nalbums, artist.albumCount);
            }
            if (artist.songCount > 0) {
                subtitle += MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, artist.songCount);
            }
            holder.subtitle.setText(subtitle);
            ArtworkManager.loadImage(new ArtInfo(artist.name, null, null), holder.artwork);
        }
    }

}
