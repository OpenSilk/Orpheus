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

package org.opensilk.music.ui3.folders;

import android.content.Context;
import android.net.Uri;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.LibraryInfo;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.library.sort.FolderTrackSortOrder;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.ActionBarMenuBaseHandler;
import org.opensilk.music.ui3.common.ActionBarMenuConfigWrapper;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleablePresenterConfig;
import org.opensilk.music.ui3.common.ItemClickDelegate;
import org.opensilk.music.ui3.common.ItemClickListener;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;

import javax.inject.Named;

import dagger.Module;
import dagger.Provides;
import mortar.MortarScope;
import rx.functions.Func2;

/**
 * Created by drew on 5/2/15.
 */
@Module
public class FoldersScreenModule {
    final FoldersScreen screen;

    public FoldersScreenModule(FoldersScreen screen) {
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
        return LibraryUris.folders(screen.libraryConfig.authority,
                screen.libraryInfo.libraryId, screen.libraryInfo.folderId);
    }

    @Provides @Named("loader_sortorder")
    public String provideLoaderSortOrder(AppPreferences preferences) {
        return preferences.getString(preferences.makePluginPrefKey(screen.libraryConfig,
                AppPreferences.FOLDER_SORT_ORDER), FolderTrackSortOrder.A_Z);
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
                if (item instanceof Folder) {
                    LibraryInfo info = screen.libraryInfo.buildUpon(item.getIdentity(), item.getName());
                    FoldersScreenFragment f = FoldersScreenFragment.ni(context, screen.libraryConfig, info);
                    presenter.getFm().replaceMainContent(f, true);
                } else if (item instanceof Track) {
                    delegate.playAllItems(context, presenter.getItems(), item);
                }
            }
        };
    }

    @Provides @ScreenScope
    public OverflowClickListener provideOverflowClickListener(FoldersOverflowHandler handler) {
        return handler;
    }

    @Provides @ScreenScope
    public ActionBarMenuConfig provideMenuConfig(
            AppPreferences appPreferences,
            ActionBarMenuConfigWrapper wrapper,
            final FoldersOverflowHandler foldersOverflowHandler
    ) {

        ActionBarMenuConfig.Builder builder = ActionBarMenuConfig.builder();

        builder.withMenu(R.menu.folder_sort_by);
        if (screen.libraryInfo.folderId != null) {
            builder.withMenus(ActionBarMenuConfig.toObject(FoldersOverflowHandler.MENUS_FOLDER));
        }

        Func2<Context, Integer, Boolean> handler = new ActionBarMenuBaseHandler(
                screen.libraryConfig,
                screen.libraryInfo,
                AppPreferences.FOLDER_SORT_ORDER,
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
                        setNewSortOrder(presenter, FolderTrackSortOrder.A_Z);
                        return true;
                    case R.id.menu_sort_by_za:
                        setNewSortOrder(presenter, FolderTrackSortOrder.Z_A);
                        return true;
                    default:
                        try {
                            return foldersOverflowHandler.onItemClicked(context,
                                    OverflowAction.valueOf(integer),
                                    Folder.builder()
                                            .setIdentity(screen.libraryInfo.folderId)
                                            .setName(screen.libraryInfo.folderName)
                                            .build());
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                }
            }
        };

        return wrapper.injectCommonItems(builder.setActionHandler(handler).build());
    }
}
