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

package org.opensilk.music.ui3.gallery;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.sort.GenreSortOrder;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;
import org.opensilk.music.ui3.common.OpenProfileItemClickListener;
import org.opensilk.music.ui3.profile.ProfileScreen;
import org.opensilk.music.ui3.profile.GenreDetailsScreen;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class GenresScreenModule {
    final GenresScreen screen;

    public GenresScreenModule(GenresScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri() {
        return screen.loaderUri;
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
    public ItemClickListener provideItemClickListener(ActivityResultsController activityResultsController) {
        return new OpenProfileItemClickListener(activityResultsController, new OpenProfileItemClickListener.ProfileScreenFactory() {
            @Override
            public ProfileScreen call(Model model) {
                return new GenreDetailsScreen(((Genre)model));
            }
        });
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri, final ActivityResultsController activityResultsController) {
        return new MenuHandlerImpl(loaderUri, activityResultsController) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenu(R.menu.genre_sort_by, menuInflater, menu);
                inflateMenu(R.menu.view_as, menuInflater, menu);
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, GenreSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, GenreSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_number_of_songs:
                        setNewSortOrder(presenter, GenreSortOrder.MOST_TRACKS);
                        return true;
                    case R.id.menu_sort_by_number_of_albums:
                        setNewSortOrder(presenter, GenreSortOrder.MOST_ALBUMS);
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
                inflateMenus(menuInflater, menu,
                        R.menu.add_to_queue,
                        R.menu.play_all,
                        R.menu.play_next,
                        R.menu.add_to_playlist
                );
                return true;
            }

            @Override
            public boolean onActionMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                List<Model> list = presenter.getSelectedItems();
                List<Uri> uris = new ArrayList<>(list.size());
                for (Model b : list) {
                    uris.add(((Genre)b).getTracksUri());
                }
                switch (menuItem.getItemId()) {
                    case R.id.add_to_queue: {
                        addToQueueFromTracksUris(context, presenter, uris);
                        return true;
                    }
                    case R.id.play_all: {
                        playFromTracksUris(context, presenter, uris);
                        return true;
                    }
                    case R.id.play_next: {
                        playNextFromTracksUris(context, presenter, uris);
                        return true;
                    }
                    case R.id.add_to_playlist: {
                        addToPlaylistFromTracksUris(context, uris);
                        return true;
                    }
                    default:
                        return false;
                }
            }
        };
    }

}
