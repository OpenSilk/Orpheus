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

package org.opensilk.music.ui3.index.tracks;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.library.sort.TrackSortOrder;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.ActionBarMenuConfigWrapper;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleableModule;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickDelegate;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.index.IndexBaseMenuHandler;

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
public class TracksScreenModule {
    final TracksScreen screen;

    public TracksScreenModule(TracksScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri(@Named("IndexProviderAuthority") String authority) {
        return IndexUris.tracks(authority);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.TRACK_SORT_ORDER), TrackSortOrder.A_Z);
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
            AppPreferences appPreferences
    ) {

        Func2<Context, Integer, Boolean> handler = new IndexBaseMenuHandler(
                AppPreferences.TRACK_SORT_ORDER,
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
                        return false;
                }
            }
        };

        return ActionBarMenuConfig.builder()
                .withMenu(R.menu.song_sort_by)
                .setActionHandler(handler)
                .build();
    }
}
