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

package org.opensilk.music.ui3.index.artists;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.sort.ArtistSortOrder;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;
import org.opensilk.music.ui3.profile.artist.ArtistDetailsScreen;
import org.opensilk.music.ui3.profile.ProfileActivity;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class ArtistsScreenModule {
    final ArtistsScreen screen;

    public ArtistsScreenModule(ArtistsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.artists(authority);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            MenuHandler menuConfig
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(true)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Model item) {
                ProfileActivity.startSelf(context, new ArtistDetailsScreen((Artist)item));
            }
        };
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri) {
        return new MenuHandlerImpl(loaderUri) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenu(R.menu.artist_sort_by, menuInflater, menu);
                inflateMenu(R.menu.view_as, menuInflater, menu);
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, ArtistSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, ArtistSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_number_of_songs:
                        setNewSortOrder(presenter, ArtistSortOrder.MOST_TRACKS);
                        return true;
                    case R.id.menu_sort_by_number_of_albums:
                        setNewSortOrder(presenter, ArtistSortOrder.MOST_ALBUMS);
                        return true;
                    case R.id.menu_view_as_simple:
                        updateLayout(presenter, AppPreferences.SIMPLE);
                        return true;
                    case R.id.menu_view_as_grid:
                        updateLayout(presenter, AppPreferences.GRID);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onBuildActionMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                return false;
            }
        };
    }
}
