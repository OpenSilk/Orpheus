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

package org.opensilk.music.ui3.index.albums;

import android.content.Context;
import android.net.Uri;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.sort.AlbumSortOrder;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.index.IndexBaseMenuHandler;
import org.opensilk.music.ui3.profile.ProfileActivity;
import org.opensilk.music.ui3.index.albumdetails.AlbumDetailsScreen;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func2;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class AlbumsScreenModule {
    final AlbumsScreen screen;

    public AlbumsScreenModule(AlbumsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.albums(authority);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.ALBUM_SORT_ORDER), AlbumSortOrder.A_Z);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            AppPreferences preferences,
            ItemClickListener itemClickListener,
            ActionBarMenuConfig menuConfig
    ) {
        boolean grid = preferences.isGrid(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.ALBUM_LAYOUT), AppPreferences.GRID);
        return BundleablePresenterConfig.builder()
                .setWantsGrid(grid)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Bundleable item) {
                ProfileActivity.startSelf(context, new AlbumDetailsScreen((Album)item));
            }
        };
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
            AppPreferences preferences
    ) {

        Func2<Context, Integer, Boolean> actionHandler = new IndexBaseMenuHandler(
                AppPreferences.ALBUM_SORT_ORDER,
                AppPreferences.ALBUM_LAYOUT,
                preferences
        ) {
            @Override
            public Boolean call(Context context, Integer integer) {
                MortarScope scope = MortarScope.findChild(context, screen.getName());
                BundleableComponent component = DaggerService.getDaggerComponent(scope);
                BundleablePresenter presenter = component.presenter();
                switch (integer) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, AlbumSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, AlbumSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_artist:
                        setNewSortOrder(presenter, AlbumSortOrder.ARTIST);
                        return true;
                    case R.id.menu_sort_by_year:
                        setNewSortOrder(presenter, AlbumSortOrder.NEWEST);
                        return true;
                    case R.id.menu_sort_by_number_of_songs:
                        setNewSortOrder(presenter, AlbumSortOrder.MOST_TRACKS);
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
        };

        return ActionBarMenuConfig.builder()
                .withMenu(R.menu.album_sort_by)
                .withMenu(R.menu.view_as)
                .setActionHandler(actionHandler)
                .build();
    }

}
