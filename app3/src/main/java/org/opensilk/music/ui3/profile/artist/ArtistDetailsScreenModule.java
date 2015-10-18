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

package org.opensilk.music.ui3.profile.artist;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.model.BioSummary;
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
import org.opensilk.music.ui3.profile.bio.BioScreen;
import org.opensilk.music.ui3.profile.tracklist.TrackListScreen;
import org.opensilk.music.ui3.ProfileActivity;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;

/**
 * Created by drew on 5/5/15.
 */
@Module
public class ArtistDetailsScreenModule {
    final ArtistDetailsScreen screen;

    public ArtistDetailsScreenModule(ArtistDetailsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.artistDetails(screen.artist);
    }

    @Provides @Named("profile_heros")
    public Boolean provideWantMultiHeros() {
        return false;
    }

    @Provides @Named("profile_heros")
    public List<ArtInfo> provideHeroArtinfos() {
        return Collections.singletonList(ArtInfo.forArtist(screen.artist.getName(), null));
    }

    @Provides @Named("profile_title")
    public String provideProfileTitle() {
        return screen.artist.getName();
    }

    @Provides @Named("profile_subtitle")
    public String provideProfileSubTitle(@ForApplication Context context) {
        String subtitle = "";
        if (screen.artist.getAlbumCount() > 0) {
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nalbums, screen.artist.getAlbumCount());
        }
        if (screen.artist.getTrackCount() > 0) {
            if (!TextUtils.isEmpty(subtitle)) subtitle += ", ";
            subtitle += UtilsCommon.makeLabel(context, R.plurals.Nsongs, screen.artist.getTrackCount());
        }
        return subtitle;
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
                    ProfileActivity.startSelf(context, new AlbumDetailsScreen((Album)item));
                } else if (item instanceof TrackList) {
                    ProfileActivity.startSelf(context, new TrackListScreen((TrackList)item));
                } else if (item instanceof BioSummary) {
                    ProfileActivity.startSelf(context, new BioScreen(provideHeroArtinfos(), (BioSummary)item));
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
                        R.menu.artist_album_sort_by,
                        R.menu.view_as
//                        ,R.menu.popup_play_next,
//                        R.menu.popup_add_to_queue
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
                        Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_LONG).show();
                        //TODO
                        return true;
                    case R.id.menu_view_as_simple:
                        updateLayout(presenter, AppPreferences.SIMPLE);
                        return true;
                    case R.id.menu_view_as_grid:
                        updateLayout(presenter, AppPreferences.GRID);
                        return true;
                    case R.id.play_next:
                        playItemsNext(presenter);
                        return true;
                    case R.id.add_to_queue:
                        addItemsToQueue(presenter);
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
