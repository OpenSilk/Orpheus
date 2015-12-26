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

package org.opensilk.music.ui3.profile;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.model.BioSummary;
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
import org.opensilk.music.ui3.common.OpenProfileItemClickListener;
import org.opensilk.music.ui3.common.UtilsCommon;

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import rx.functions.Action1;
import rx.functions.Action2;

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
    public Uri provideLoaderUri() {
        return screen.artist.getDetailsUri();
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
                .setAllowLongPressSelection(false)
                .setMenuConfig(menuConfig)
                .setFabClickAction(new Action2<Context, BundleablePresenter>() {
                    @Override
                    public void call(Context context, final BundleablePresenter presenter) {
                        UtilsCommon.addTracksToQueue(context,
                                Collections.singletonList(screen.artist.getTracksUri()),
                                new Action1<List<Uri>>() {
                                    @Override
                                    public void call(List<Uri> uris) {
                                        presenter.getPlaybackController().playAll(uris, 0);
                                    }
                                });
                    }
                })
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener(ActivityResultsController activityResultsController) {
        return new OpenProfileItemClickListener(activityResultsController, new OpenProfileItemClickListener.ProfileScreenFactory() {
            @Override
            public ProfileScreen call(Model item) {
                if (item instanceof Album) {
                    return new AlbumDetailsScreen((Album)item);
                } else if (item instanceof TrackList) {
                    return new TrackListScreen((TrackList)item);
                } else if (item instanceof BioSummary) {
                    return new BioScreen(provideHeroArtinfos(), (BioSummary)item);
                } else {
                    throw new IllegalArgumentException("Unkown model type " + item.getClass());
                }
            }
        });
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri, final ActivityResultsController activityResultsController) {
        return new MenuHandlerImpl(loaderUri, activityResultsController) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenus(menuInflater, menu,
                        R.menu.artist_album_sort_by,
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
