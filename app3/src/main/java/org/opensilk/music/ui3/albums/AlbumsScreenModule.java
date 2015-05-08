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

package org.opensilk.music.ui3.albums;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.PopupMenu;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.AlbumSortOrder;
import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.MusicActivityComponent;
import org.opensilk.music.ui3.ProfileActivity;
import org.opensilk.music.ui3.albumsprofile.AlbumsProfileScreen;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func0;
import rx.functions.Func1;
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
    public Uri provideLoaderUri() {
        return LibraryUris.albums(screen.libraryConfig.authority,
                screen.libraryInfo.libraryId);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePluginPrefKey(screen.libraryConfig,
                AppPreferences.ALBUM_SORT_ORDER), AlbumSortOrder.A_Z);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            AppPreferences preferences,
            ItemClickListener itemClickListener,
            OverflowClickListener overflowClickListener,
            ActionBarMenuConfig menuConfig
    ) {
        boolean grid = preferences.isGrid(preferences.makePluginPrefKey(screen.libraryConfig,
                AppPreferences.ALBUM_LAYOUT), AppPreferences.GRID);
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
                ProfileActivity.startSelf(context, new AlbumsProfileScreen(screen.libraryConfig,
                        screen.libraryInfo.buildUpon(item.getIdentity(), item.getName()), (Album)item));
            }
        };
    }

    @Provides @ScreenScope
    public OverflowClickListener provideOverflowClickListener() {
        return new OverflowClickListener() {
            @Override
            public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {
                final int[] MENUS = new int[]{
                        R.menu.popup_play_all,
                        R.menu.popup_shuffle_all,
                        R.menu.popup_add_to_queue,
//                                R.menu.popup_add_to_playlist,
//                                R.menu.popup_more_by_artist,
//                                R.menu.popup_delete,
                };
                for (int ii : MENUS) {
                    m.inflate(ii);
                }
            }

            @Override
            public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {
                BundleableComponent component = DaggerService.getDaggerComponent(context);
                PlaybackController playbackController = component.playbackController();
                AppPreferences appPreferences = component.appPreferences();
                Uri uri = LibraryUris.albumTracks(screen.libraryConfig.authority, screen.libraryInfo.libraryId, item.getIdentity());
                String sortOrder = appPreferences.getString(appPreferences.makePluginPrefKey(screen.libraryConfig,
                        AppPreferences.ALBUM_TRACK_SORT_ORDER), TrackSortOrder.PLAYORDER);
                switch (action) {
                    case PLAY_ALL:
                        playbackController.playTracksFrom(uri, 0, sortOrder);
                        return true;
                    case SHUFFLE_ALL:
                        playbackController.shuffleTracksFrom(uri);
                        return true;
                    case ADD_TO_QUEUE:
                        playbackController.addTracksToQueueFrom(uri, sortOrder);
                        return true;
                    case ADD_TO_PLAYLIST:
                        //TODO
                        return true;
                    case MORE_BY_ARTIST:
                        //TODO
                        return true;
                    case DELETE:
                        //TODO
                        return true;
                    default:
                        return false;
                }
            }
        };
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig() {

        Func2<Context, Integer, Boolean> actionHandler = new Func2<Context, Integer, Boolean>() {
            @Override
            public Boolean call(Context context, Integer integer) {
                MusicActivityComponent component = DaggerService.getDaggerComponent(context);
                AppPreferences appPreferences = component.appPreferences();
                MortarScope scope = MortarScope.findChild(context, screen.getName());
                BundleableComponent component1 = DaggerService.getDaggerComponent(scope);
                BundleablePresenter presenter = component1.presenter();
                switch (integer) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(AlbumSortOrder.A_Z, appPreferences, presenter);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(AlbumSortOrder.Z_A, appPreferences, presenter);
                        return true;
                    case R.id.menu_sort_by_artist:
                        setNewSortOrder(AlbumSortOrder.ARTIST, appPreferences, presenter);
                        return true;
                    case R.id.menu_sort_by_year:
                        setNewSortOrder(AlbumSortOrder.NEWEST, appPreferences, presenter);
                        return true;
                    case R.id.menu_sort_by_number_of_songs:
                        setNewSortOrder(AlbumSortOrder.MOST_TRACKS, appPreferences, presenter);
                        return true;
                    case R.id.menu_view_as_simple:
                        updateLayout(AppPreferences.SIMPLE, appPreferences, presenter);
                        return true;
                    case R.id.menu_view_as_grid:
                        updateLayout(AppPreferences.GRID, appPreferences, presenter);
                        return true;
                    default:
                        return false;
                }
            }

            void setNewSortOrder(String sortorder, AppPreferences settings, BundleablePresenter presenter) {
                settings.putString(settings.makePluginPrefKey(screen.libraryConfig, AppPreferences.ALBUM_SORT_ORDER), sortorder);
                presenter.getLoader().setSortOrder(sortorder);
                presenter.reload();
            }

            void updateLayout(String kind, AppPreferences settings, BundleablePresenter presenter) {
                settings.putString(settings.makePluginPrefKey(screen.libraryConfig, AppPreferences.ALBUM_LAYOUT), kind);
                presenter.setWantsGrid(StringUtils.equals(kind, AppPreferences.GRID));
                presenter.resetRecyclerView();
            }
        };

        return ActionBarMenuConfig.builder()
                .withMenu(R.menu.album_sort_by)
                .withMenu(R.menu.view_as)
                .setActionHandler(actionHandler)
                .build();
    }
}
