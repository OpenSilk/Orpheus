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

package org.opensilk.music.ui3.playlist;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.jakewharton.rxbinding.view.ViewClickEvent;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActivityResultsController;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.R;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.sort.PlaylistSortOrder;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.MenuHandler;
import org.opensilk.music.ui3.common.MenuHandlerImpl;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import hugo.weaving.DebugLog;
import rx.functions.Action1;

/**
 * Created by drew on 10/24/15.
 */
@Module
public class PlaylistChooseScreenModule {
    final PlaylistChooseScreen screen;

    public PlaylistChooseScreenModule(PlaylistChooseScreen screen) {
        this.screen = screen;
    }

    @Provides @Named("loader_uri")
    public Uri provideLoaderUri() {
        return screen.loaderUri;
    }

    @Provides @ScreenScope
    public BundleablePresenterConfig providePresenterConfig(
            ItemClickListener itemClickListener,
            MenuHandler menuConfig,
            @ForApplication Context context
    ) {
        return BundleablePresenterConfig.builder()
                .setWantsGrid(false)
                .setItemClickListener(itemClickListener)
                .setMenuConfig(menuConfig)
                .setToolbarTitle(context.getString(R.string.title_playlists))
                .build();
    }

    @Provides @ScreenScope
    public ItemClickListener provideItemClickListener() {
        return new ItemClickListener() {
            @Override
            public void onItemClicked(final BundleablePresenter presenter, final Context context, final Model item) {
                presenter.getFm().showDialog(PlaylistProgressScreenFragment.addTo(
                        item.getUri(), screen.listKind, screen.uris));
            }
        };
    }

    @Provides @ScreenScope
    public MenuHandler provideMenuHandler(@Named("loader_uri") final Uri loaderUri,
                                          final ActivityResultsController activityResultsController) {
        return new MenuHandlerImpl(loaderUri, activityResultsController) {
            @Override
            public boolean onBuildMenu(BundleablePresenter presenter, MenuInflater menuInflater, Menu menu) {
                inflateMenus(menuInflater, menu,
                        R.menu.playlist_sort_by
                );
                return true;
            }

            @Override
            public boolean onMenuItemClicked(BundleablePresenter presenter, Context context, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.menu_sort_by_az:
                        setNewSortOrder(presenter, PlaylistSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, PlaylistSortOrder.Z_A);
                        return true;
                    case R.id.menu_sort_by_date_added:
                        setNewSortOrder(presenter, PlaylistSortOrder.LAST_ADDED);
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

    @Provides @Named("fabaction")
    Action1<ViewClickEvent> provideFabClickAction(final FragmentManagerOwner fm) {
        return new Action1<ViewClickEvent>() {
            @Override
            @DebugLog
            public void call(ViewClickEvent viewClickEvent) {
                fm.showDialog(PlaylistCreateScreenFragment.ni(screen.loaderUri.getAuthority()));
            }
        };
    }

}
