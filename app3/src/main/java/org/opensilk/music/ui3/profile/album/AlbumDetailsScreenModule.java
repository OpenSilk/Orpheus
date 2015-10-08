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

package org.opensilk.music.ui3.profile.album;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.sort.TrackSortOrder;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.PlayAllItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class AlbumDetailsScreenModule {
    final AlbumDetailsScreen screen;

    public AlbumDetailsScreenModule(AlbumDetailsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.albumDetails(screen.album);
    }

    @Provides @Named("profile_heros")
    public Boolean provideWantMultiHeros() {
        return false;
    }

    @Provides @Named("profile_heros")
    public List<ArtInfo> provideHeroArtinfos() {
        final Album album = screen.album;
        return Collections.singletonList(ArtInfo.forAlbum(album.getArtistName(),
                album.getName(), album.getArtworkUri()));
    }

    @Provides @Named("profile_title")
    public String provideProfileTitle() {
        return screen.album.getName();
    }

    @Provides @Named("profile_subtitle")
    public String provideProfileSubTitle() {
        return screen.album.getArtistName();
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            MenuHandler menuConfig
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .setDefaultSortOrder(TrackSortOrder.PLAYORDER)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new PlayAllItemClickListener();
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri) {
        return new MenuHandlerImpl(loaderUri) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenu(R.menu.album_song_sort_by, menuInflater, menu);
                inflateMenu(R.menu.add_to_queue, menuInflater, menu);
                inflateMenu(R.menu.play_next, menuInflater, menu);
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_track_list:
                        setNewSortOrder(presenter, TrackSortOrder.PLAYORDER);
                        return true;
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, TrackSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, TrackSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_duration:
                        setNewSortOrder(presenter, TrackSortOrder.LONGEST);
                        return true;
                    case R.id.menu_sort_by_artist:
                        setNewSortOrder(presenter, TrackSortOrder.ARTIST);
                        return true;
                    case R.id.add_to_queue:
                        addItemsToQueue(presenter);
                        return true;
                    case R.id.play_next:
                        playItemsNext(presenter);
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public boolean onBuildActionMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenus(menuInflater, menu,
                        R.menu.add_to_queue,
                        R.menu.play_next
                );
                return true;
            }

            @Override
            public boolean onActionMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.add_to_queue:
                        addSelectedItemsToQueue(presenter);
                        return true;
                    case R.id.play_next:
                        playSelectedItemsNext(presenter);
                        return true;
                    default:
                        return false;
                }
            }
        };
    }

}
