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

package org.opensilk.music.ui3.genres;

import android.content.Context;
import android.net.Uri;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.GenreSortOrder;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.profile.ProfileActivity;
import org.opensilk.music.ui3.common.ActionBarMenuBaseHandler;
import org.opensilk.music.ui3.common.ActionBarMenuConfigWrapper;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.genresprofile.GenresProfileScreen;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func2;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class GenresScreenModule {
    final GenresScreen screen;

    public GenresScreenModule(GenresScreen screen) {
        this.screen = screen;
    }

    @Provides
    public LibraryConfig provideLibraryConfig() {
        return screen.libraryConfig;
    }

    @Provides
    public LibraryInfo provideLibraryInfo() {
        return screen.libraryInfo;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri() {
        return LibraryUris.genres(screen.libraryConfig.authority,
                screen.libraryInfo.libraryId);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePluginPrefKey(screen.libraryConfig,
                AppPreferences.GENRE_SORT_ORDER), GenreSortOrder.A_Z);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            AppPreferences preferences,
            ItemClickListener itemClickListener,
            ActionBarMenuConfig menuConfig
    ) {
        boolean grid = preferences.isGrid(preferences.makePluginPrefKey(screen.libraryConfig,
                AppPreferences.GENRE_LAYOUT), AppPreferences.GRID);
        return BundleablePresenterConfig.builder()
                .setWantsGrid(grid)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .build();
    }

    @Provides @Named("presenter_wantGrid")
    public Boolean provideWantGrid(AppPreferences preferences) {
        return preferences.isGrid(preferences.makePluginPrefKey(screen.libraryConfig, AppPreferences.GENRE_LAYOUT), AppPreferences.GRID);
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Bundleable item) {
                ProfileActivity.startSelf(context, new GenresProfileScreen(screen.libraryConfig,
                        screen.libraryInfo.buildUpon(item.getIdentity(), item.getName()), (Genre)item));
            }
        };
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
            AppPreferences appPreferences,
            ActionBarMenuConfigWrapper wrapper
    ) {

        Func2<Context, Integer, Boolean> handler = new ActionBarMenuBaseHandler(
                screen.libraryConfig,
                screen.libraryInfo,
                AppPreferences.GENRE_SORT_ORDER,
                AppPreferences.GENRE_LAYOUT,
                appPreferences
        ) {
            @Override
            public Boolean call(Context context, Integer integer) {
                MortarScope scope = MortarScope.findChild(context, screen.getName());
                BundleableComponent component = DaggerService.getDaggerComponent(scope);
                BundleablePresenter presenter = component.presenter();
                switch (integer) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, GenreSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, GenreSortOrder.Z_A);
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

        return wrapper.injectCommonItems(ActionBarMenuConfig.builder()
                .withMenu(R.menu.genre_sort_by)
                .withMenu(R.menu.view_as)
                .setActionHandler(handler)
                .build());
    }
}
