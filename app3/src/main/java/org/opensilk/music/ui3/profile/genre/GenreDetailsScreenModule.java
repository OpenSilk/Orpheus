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

package org.opensilk.music.ui3.profile.genre;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.bundleable.Bundleable;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.TrackList;
import org.opensilk.music.model.sort.AlbumSortOrder;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;
import org.opensilk.music.ui3.common.UtilsCommon;
import org.opensilk.music.ui3.profile.album.AlbumDetailsScreen;
import org.opensilk.music.ui3.profile.tracklist.TrackListScreen;
import org.opensilk.music.ui3.profile.ProfileActivity;

import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class GenreDetailsScreenModule {
    final GenreDetailsScreen screen;

    public GenreDetailsScreenModule(GenreDetailsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.genreDetails(screen.genre);
    }

    @Provides @Named("trackcollection_sortorderpref")
    public String provideTrackCollectionSortOrderPref() {
        return AppPreferences.GENRE_TRACK_SORT_ORDER;
    }

    @Provides @Named("profile_heros")
    public Boolean provideWantMultiHeros() {
        return screen.genre.getArtInfos().size() > 1;
    }

    @Provides @Named("profile_heros")
    public List<ArtInfo> provideHeroArtinfos() {
        return screen.genre.getArtInfos();
    }

    @Provides @Named("profile_title")
    public String provideProfileTitle() {
        return screen.genre.getName();
    }

    @Provides @Named("profile_subtitle")
    public String provideProfileSubTitle(@ForApplication Context context) {
        return UtilsCommon.makeLabel(context, R.plurals.Nalbums, screen.genre.getAlbumsCount())
                + ", " + UtilsCommon.makeLabel(context, R.plurals.Nsongs, screen.genre.getTracksCount());
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
                if (item instanceof Album) {
                    ProfileActivity.startSelf(context, new AlbumDetailsScreen((Album) item));
                } else if (item instanceof TrackList) {
                    ProfileActivity.startSelf(context, new TrackListScreen((TrackList) item));
                }
            }
        };
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri) {
        return new MenuHandlerImpl(loaderUri) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenus(menuInflater, menu,
                        R.menu.genre_album_sort_by,
                        R.menu.view_as
                        );
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, AlbumSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, AlbumSortOrder.Z_A);
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
