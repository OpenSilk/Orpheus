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

package org.opensilk.music.ui3.index.artistdetails;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.TrackList;
import org.opensilk.music.model.sort.AlbumSortOrder;
import org.opensilk.bundleable.Bundleable;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandlerImpl;
import org.opensilk.music.ui3.common.UtilsCommon;
import org.opensilk.music.ui3.index.IndexBaseMenuHandler;
import org.opensilk.music.ui3.index.albumdetails.AlbumDetailsScreen;
import org.opensilk.music.ui3.index.trackcollection.TrackCollectionScreen;
import org.opensilk.music.ui3.profile.ProfileActivity;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func2;

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

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.ARTIST_ALBUM_SORT_ORDER), AlbumSortOrder.A_Z);
    }

    @Provides @Named("trackcollection_sortorderpref")
    public String provideTrackCollectionSortOrderPref() {
        return AppPreferences.ARTIST_TRACK_SORT_ORDER;
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
            @ForApplication Context context,
            AppPreferences preferences,
            ItemClickListener itemClickListener,
            ActionBarMenuConfig menuConfig
    ) {
        boolean grid = preferences.isGrid(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.ALBUM_LAYOUT), AppPreferences.GRID);
//        TrackCollection allTracks = TrackCollection.builder()
//                .setName(context.getString(R.string.title_all_songs))
//                .setTracksUri(LibraryUris.artistTracks(screen.libraryConfig.authority,
//                        screen.libraryInfo.libraryId, screen.artist.identity))
//                .setTrackCount(screen.artist.trackCount)
//                .setAlbumCount(screen.artist.albumCount)
//                .addArtInfo(ArtInfo.forArtist(screen.artist.name, null))
//                .build();
        return BundleablePresenterConfig.builder()
                .setWantsGrid(grid)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
//                .addLoaderSeed(allTracks)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Bundleable item) {
                if (item instanceof Album) {
                    ProfileActivity.startSelf(context, new AlbumDetailsScreen((Album)item));
                } else if (item instanceof TrackList) {
                    ProfileActivity.startSelf(context, new TrackCollectionScreen((TrackList)item,
                            provideTrackCollectionSortOrderPref()));
                }
            }
        };
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
            final AppPreferences appPreferences
    ) {

        Func2<Context, Integer, Boolean> handler = new IndexBaseMenuHandler(
                AppPreferences.ARTIST_ALBUM_SORT_ORDER,
                AppPreferences.ARTIST_ALBUM_LAYOUT,
                appPreferences
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
                    default:
                            return false;
                }
            }
        };

        return ActionBarMenuConfig.builder()
                .withMenu(R.menu.artist_album_sort_by)
                .withMenu(R.menu.view_as)
                .withMenus(ActionBarMenuConfig.toObject(MenuHandlerImpl.ARTISTS))
                .setActionHandler(handler)
                .build();
    }
}
