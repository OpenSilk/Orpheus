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

package org.opensilk.music.ui3.index.genredetails;

import android.content.Context;
import android.net.Uri;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.library.sort.AlbumSortOrder;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.TrackList;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.BundleableModule;
import org.opensilk.music.ui3.index.IndexBaseMenuHandler;
import org.opensilk.music.ui3.index.IndexOverflowHandler;
import org.opensilk.music.ui3.profile.ProfileActivity;
import org.opensilk.music.ui3.index.albumdetails.AlbumDetailsScreen;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;
import org.opensilk.music.ui3.common.OverflowHandler;
import org.opensilk.music.ui3.common.UtilsCommon;
import org.opensilk.music.ui3.index.trackcollection.TrackCollectionOverflowHandler;
import org.opensilk.music.ui3.index.trackcollection.TrackCollectionScreen;

import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func2;

/**
 * Created by drew on 5/5/15.
 */
@Module(
        includes = BundleableModule.class
)
public class GenreDetailsScreenModule {
    final GenreDetailsScreen screen;

    public GenreDetailsScreenModule(GenreDetailsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.genreDetails(screen.genre);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.GENRE_ALBUM_SORT_ORDER), AlbumSortOrder.A_Z);
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
        return screen.genre.getDisplayName();
    }

    @Provides @Named("profile_subtitle")
    public String provideProfileSubTitle(@ForApplication Context context) {
        return UtilsCommon.makeLabel(context, R.plurals.Nalbums, screen.genre.getAlbumsCount())
                + ", " + UtilsCommon.makeLabel(context, R.plurals.Nsongs, screen.genre.getTracksCount());
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            @ForApplication Context context,
            AppPreferences preferences,
            ItemClickListener itemClickListener,
            OverflowClickListener overflowClickListener,
            ActionBarMenuConfig menuConfig
    ) {
        boolean grid = preferences.isGrid(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.GENRE_ALBUM_LAYOUT), AppPreferences.GRID);
//        TrackCollection allTracks = TrackCollection.builder()
//                .setName(context.getString(R.string.title_all_songs))
//                .setTracksUri(LibraryUris.genreTracks(screen.libraryConfig.authority,
//                        screen.libraryInfo.libraryId, screen.genre.identity))
//                .setTrackCount(screen.genre.trackUris.size())
//                .setAlbumCount(screen.genre.albumUris.size())
//                .addArtInfos(screen.genre.artInfos)
//                .build();
        return BundleablePresenterConfig.builder()
                .setWantsGrid(grid)
                .setItemClickListener(itemClickListener)
                .setOverflowClickListener(overflowClickListener)
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
                    ProfileActivity.startSelf(context, new AlbumDetailsScreen((Album) item));
                } else if (item instanceof TrackList) {
                    ProfileActivity.startSelf(context, new TrackCollectionScreen((TrackList) item,
                            AppPreferences.GENRE_TRACK_SORT_ORDER));
                }
            }
        };
    }

    @Provides @ScreenScope
    public OverflowClickListener provideOverflowClickListener(
            final IndexOverflowHandler albumsOverflowHandler,
            final TrackCollectionOverflowHandler trackCollectionOverflowHandler
    ) {
        return new OverflowClickListener() {
            @Override
            public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {
                if (item instanceof Album) {
                    albumsOverflowHandler.onBuildMenu(context, m, item);
                } else if (item instanceof TrackList) {
                    trackCollectionOverflowHandler.onBuildMenu(context, m, item);
                }
            }

            @Override
            public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {
                if (item instanceof Album) {
                    return albumsOverflowHandler.onItemClicked(context, action, item);
                } else if (item instanceof TrackList) {
                    return trackCollectionOverflowHandler.onItemClicked(context, action, item);
                } else {
                    return false;
                }
            }
        };
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
            AppPreferences appPreferences,
            final IndexOverflowHandler genresOverflowHandler
    ) {

        Func2<Context, Integer, Boolean> handler = new IndexBaseMenuHandler(
                AppPreferences.GENRE_ALBUM_SORT_ORDER,
                AppPreferences.GENRE_ALBUM_LAYOUT,
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
                        setNewSortOrder(presenter, AlbumSortOrder.MOST_TRACKS);
                        return true;
                    case R.id.menu_view_as_simple:
                        updateLayout(presenter, AppPreferences.SIMPLE);
                        return true;
                    case R.id.menu_view_as_grid:
                        updateLayout(presenter, AppPreferences.GRID);
                        return true;
                    default:
                        try {
                            return genresOverflowHandler.onItemClicked(context,
                                    OverflowAction.valueOf(integer), screen.genre);
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                }
            }
        };

        return ActionBarMenuConfig.builder()
                .withMenu(R.menu.genre_album_sort_by)
                .withMenu(R.menu.view_as)
                .withMenus(ActionBarMenuConfig.toObject(OverflowHandler.GENRES))
                .setActionHandler(handler)
                .build();
    }
}
