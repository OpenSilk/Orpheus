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

package org.opensilk.music.ui3.tracksprofile;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.ActionBarMenuBaseHandler;
import org.opensilk.music.ui3.common.ActionBarMenuConfigWrapper;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickDelegate;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;
import org.opensilk.music.ui3.common.UtilsCommon;
import org.opensilk.music.ui3.tracks.TracksOverflowHandler;

import java.util.List;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func2;

/**
 * Created by drew on 5/12/15.
 */
@Module
public class TracksProfileScreenModule {
    final TracksProfileScreen screen;

    public TracksProfileScreenModule(TracksProfileScreen screen) {
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
        return screen.trackCollection.tracksUri;
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePluginPrefKey(screen.libraryConfig,
                screen.sortOrderPref), TrackSortOrder.A_Z);
    }

    @Provides @Named("trackcollection_sortorderpref")
    public String provideTrackCollectionSortOrderPref() {
        return screen.sortOrderPref;
    }

    @Provides @Named("profile_heros")
    public Boolean provideWantMultiHeros() {
        return screen.trackCollection.artInfos.size() > 1;
    }

    @Provides @Named("profile_heros")
    public List<ArtInfo> provideHeroArtinfos() {
        return screen.trackCollection.artInfos;
    }

    @Provides @Named("profile_title")
    public String provideProfileTitle() {
        return screen.trackCollection.name;
    }

    @Provides @Named("profile_subtitle")
    public String provideProfileSubTitle(@ForApplication Context context) {
        return UtilsCommon.makeLabel(context, R.plurals.Nalbums, screen.trackCollection.albumCount)
                + ", " + UtilsCommon.makeLabel(context, R.plurals.Nsongs, screen.trackCollection.trackCount);
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            OverflowClickListener overflowClickListener,
            ActionBarMenuConfig menuConfig
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
                .setItemClickListener(itemClickListener)
                .setOverflowClickListener(overflowClickListener)
                .setMenuConfig(menuConfig)
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener(final ItemClickDelegate delegate) {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(BundleablePresenter presenter, Context context, Bundleable item) {
                delegate.playAllItems(context, presenter.getItems(), item);
            }
        };
    }

    @Provides @ScreenScope
    public OverflowClickListener provideOverflowClickListener(final TracksOverflowHandler delegate) {
        return delegate;
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
            AppPreferences appPreferences,
            ActionBarMenuConfigWrapper wrapper,
            final TrackCollectionOverflowHandler trackCollectionOverflowHandler
    ) {

        Func2<Context, Integer, Boolean> handler = new ActionBarMenuBaseHandler(
                screen.libraryConfig,
                screen.libraryInfo,
                screen.sortOrderPref,
                null,
                appPreferences
        ) {
            @Override
            public Boolean call(Context context, Integer integer) {
                MortarScope scope = MortarScope.findChild(context, screen.getName());
                BundleableComponent component = DaggerService.getDaggerComponent(scope);
                BundleablePresenter presenter = component.presenter();
                switch (integer) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, TrackSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, TrackSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_artist:
                        setNewSortOrder(presenter, TrackSortOrder.ARTIST);
                        return true;
                    case R.id.menu_sort_by_album:
                        setNewSortOrder(presenter, TrackSortOrder.ALBUM);
                        return true;
                    case R.id.menu_sort_by_year:
                        Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_LONG).show();
                        //TODO
                        return true;
                    case R.id.menu_sort_by_duration:
                        setNewSortOrder(presenter, TrackSortOrder.LONGEST);
                        return true;
                    case R.id.menu_sort_by_filename:
                        Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_LONG).show();
                        //TODO
                        return true;
                    case R.id.menu_sort_by_date_added:
                        Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_LONG).show();
                        //TODO
                        return true;
                    default:
                        try {
                            return trackCollectionOverflowHandler.onItemClicked(context,
                                    OverflowAction.valueOf(integer), screen.trackCollection);
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                }
            }
        };

        return wrapper.injectCommonItems(ActionBarMenuConfig.builder()
                .withMenu(R.menu.popup_play_all)
                .withMenu(R.menu.popup_shuffle_all)
                .withMenu(R.menu.popup_add_to_queue)
                .withMenu(R.menu.song_sort_by)
                .setActionHandler(handler)
                .build());
    }
}
