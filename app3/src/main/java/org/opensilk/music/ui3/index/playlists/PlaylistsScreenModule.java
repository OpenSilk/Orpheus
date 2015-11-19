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

package org.opensilk.music.ui3.index.playlists;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.sort.PlaylistSortOrder;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;
import org.opensilk.music.ui3.common.OpenProfileItemClickListener;
import org.opensilk.music.ui3.profile.ProfileScreen;
import org.opensilk.music.ui3.profile.playlist.PlaylistDetailsScreen;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class PlaylistsScreenModule {
    final PlaylistsScreen screen;

    public PlaylistsScreenModule(PlaylistsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.playlists(authority);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            MenuHandler menuConfig,
            @ForApplication Context context
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(true)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .setToolbarTitle(context.getString(R.string.title_playlists))
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener(ActivityResultsController activityResultsController) {
        return new OpenProfileItemClickListener(activityResultsController, new OpenProfileItemClickListener.ProfileScreenFactory() {
            @Override
            public ProfileScreen call(Model model) {
                return new PlaylistDetailsScreen((Playlist)model);
            }
        });
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri, final ActivityResultsController activityResultsController) {
        return new MenuHandlerImpl(loaderUri, activityResultsController) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenus(menuInflater, menu,
                        R.menu.playlist_sort_by,
                        R.menu.view_as
                );
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, PlaylistSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, PlaylistSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_date_added:
                        setNewSortOrder(presenter, PlaylistSortOrder.LAST_ADDED);
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
                inflateMenus(menuInflater, menu,
                        R.menu.add_to_queue,
                        R.menu.play_all,
                        R.menu.play_next,
                        R.menu.delete
                );
                return true;
            }

            @Override
            public boolean onActionMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                List<Model> list = presenter.getSelectedItems();
                List<Uri> uris = new ArrayList<>(list.size());
                for (Model b : list) {
                    uris.add(((Playlist)b).getTracksUri());
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
                    case R.id.delete: {
                        List<Uri> plsts = new ArrayList<>(presenter.getSelectedItems().size());
                        for (Model m : presenter.getSelectedItems()) {
                            plsts.add(m.getUri());
                        }
                        presenter.getIndexClient().removePlaylists(plsts);
                        return true;
                    }
                    default:
                        return false;
                }
            }
        };
    }

}
