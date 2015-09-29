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

package org.opensilk.music.ui3.index.albumdetails;

import android.content.Context;
import android.net.Uri;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleableModule;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickDelegate;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowHandler;
import org.opensilk.music.ui3.index.IndexBaseMenuHandler;
import org.opensilk.music.ui3.index.IndexOverflowHandler;

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
@Module(
        includes = BundleableModule.class
)
public class AlbumDetailsScreenModule {
    final AlbumDetailsScreen screen;

    public AlbumDetailsScreenModule(AlbumDetailsScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.albumDetails(screen.album);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.ALBUM_TRACK_SORT_ORDER), TrackSortOrder.PLAYORDER);
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
        return screen.album.getDisplayName();
    }

    @Provides @Named("profile_subtitle")
    public String provideProfileSubTitle() {
        return screen.album.getArtistName();
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            ActionBarMenuConfig menuConfig
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
                .setItemClickListener(itemClickListener)
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
    public ActionBarMenuConfig provideMenuConfig(
            AppPreferences preferences,
            final IndexOverflowHandler albumsOverflowHandler
    ) {

    Func2<Context, Integer, Boolean> handler = new IndexBaseMenuHandler(
            AppPreferences.ALBUM_TRACK_SORT_ORDER,
            null,
            preferences
    ) {
        @Override
        public Boolean call(Context context, Integer integer) {
            MortarScope scope = MortarScope.findChild(context, screen.getName());
            BundleableComponent component = DaggerService.getDaggerComponent(scope);
            BundleablePresenter presenter = component.presenter();
            switch (integer) {
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
                default:
                    try {
                        OverflowAction action = OverflowAction.valueOf(integer);
                        boolean handled = albumsOverflowHandler.onItemClicked(context, action, screen.album);
                        return handled;
                    } catch (IllegalArgumentException e) {
                        return false;
                    }
            }
        }
    };

    return ActionBarMenuConfig.builder()
            .withMenu(R.menu.album_song_sort_by)
            .withMenus(ActionBarMenuConfig.toObject(OverflowHandler.ALBUMS))
            .setActionHandler(handler)
            .build();
    }

}
