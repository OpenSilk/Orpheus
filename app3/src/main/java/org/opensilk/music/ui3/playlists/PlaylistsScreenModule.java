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

package org.opensilk.music.ui3.playlists;

import android.content.Context;
import android.net.Uri;
import android.widget.PopupMenu;
import android.widget.Toast;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.App;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.PlaylistSortOrder;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.ProfileActivity;
import org.opensilk.music.ui3.TracksDragSwipeActivity;
import org.opensilk.music.ui3.common.ActionBarMenuBaseHandler;
import org.opensilk.music.ui3.common.ActionBarMenuConfigWrapper;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;
import org.opensilk.music.ui3.playlistsprofile.PlaylistsProfileScreen;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func2;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class PlaylistsScreenModule {
    final PlaylistsScreen screen;

    public PlaylistsScreenModule(PlaylistsScreen screen) {
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
        return LibraryUris.playlists(screen.libraryConfig.authority,
                screen.libraryInfo.libraryId);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePluginPrefKey(screen.libraryConfig,
                AppPreferences.PLAYLIST_SORT_ORDER), PlaylistSortOrder.A_Z);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            AppPreferences preferences,
            ItemClickListener itemClickListener,
            OverflowClickListener overflowClickListener,
            ActionBarMenuConfig menuConfig
    ) {
        boolean grid = preferences.isGrid(preferences.makePluginPrefKey(screen.libraryConfig,
                AppPreferences.PLAYLIST_LAYOUT), AppPreferences.GRID);
        return BundleablePresenterConfig.builder()
                .setWantsGrid(grid)
                .setItemClickListener(itemClickListener)
                .setOverflowClickListener(overflowClickListener)
                .setMenuConfig(menuConfig)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Bundleable item) {
                TracksDragSwipeActivity.startSelf(context, new PlaylistsProfileScreen(screen.libraryConfig,
                        screen.libraryInfo.buildUpon(item.getIdentity(), item.getName()), (Playlist) item));
            }
        };
    }

    @Provides @ScreenScope
    public OverflowClickListener provideOverflowClickListener(PlaylistsOverflowHandler handler) {
        return handler;
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
            AppPreferences appPreferences,
            ActionBarMenuConfigWrapper wrapper
    ) {

        Func2<Context, Integer, Boolean> handler = new ActionBarMenuBaseHandler(
                screen.libraryConfig,
                screen.libraryInfo,
                AppPreferences.PLAYLIST_SORT_ORDER,
                AppPreferences.PLAYLIST_LAYOUT,
                appPreferences
        ) {
            @Override
            public Boolean call(Context context, Integer integer) {
                MortarScope scope = MortarScope.findChild(context, screen.getName());
                BundleableComponent component = DaggerService.getDaggerComponent(scope);
                BundleablePresenter presenter = component.presenter();
                switch (integer) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, PlaylistSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, PlaylistSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_date_added:
                        Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_LONG).show();
                        //TODO
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
                .withMenu(R.menu.playlist_sort_by)
                .withMenu(R.menu.view_as)
                .setActionHandler(handler)
                .build());
    }
}
