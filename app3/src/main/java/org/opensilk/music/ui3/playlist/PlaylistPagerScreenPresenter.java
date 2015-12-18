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
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.common.ui.mortar.ActionModePresenter;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.index.provider.IndexUris;
import org.opensilk.music.library.mediastore.provider.FoldersUris;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import mortar.ViewPresenter;

/**
 * Created by drew on 12/11/15.
 */
@ScreenScope
public class PlaylistPagerScreenPresenter extends ViewPresenter<PlaylistPagerScreenView> {

    final ActionModePresenter actionModePresenter;
    final String indexAuthority;
    final String foldersAuthority;
    final AppPreferences settings;

    DelegateActionHandler delegateActionHandler;

    @Inject
    public PlaylistPagerScreenPresenter(
            ActionModePresenter actionModePresenter,
            @Named("IndexProviderAuthority") String indexAuthority,
            @Named("foldersLibraryAuthority") String foldersAuthority,
            AppPreferences settings
    ) {
        this.actionModePresenter = actionModePresenter;
        this.indexAuthority = indexAuthority;
        this.foldersAuthority = foldersAuthority;
        this.settings = settings;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        //if changing these also update playlist provider select screen
        //TODO create playlistProviderLoader to handle any declared playlistProviders
        List<PlaylistsScreen> screens = new ArrayList<>();
        screens.add(new PlaylistsScreen(IndexUris.playlists(indexAuthority),
                getView().getContext().getString(R.string.orpheus_playlists)));
        screens.add(new PlaylistsScreen(FoldersUris.playlists(foldersAuthority),
                getView().getContext().getString(R.string.folders_playlists)));
        int startpage = settings.getInt(AppPreferences.PLAYLISTS_START_PAGE,
                AppPreferences.DEFAULT_PLAYLISTS_PAGE);
        getView().setup(screens, startpage);
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (hasView()) {
            int current = getView().mViewPager.getCurrentItem();
            settings.putInt(AppPreferences.PLAYLISTS_START_PAGE, current);
        }
    }

    void updateActionBarWithChildMenuConfig(ActionBarMenuHandler menuConfig) {
        if (delegateActionHandler == null) {
            delegateActionHandler = new DelegateActionHandler();
        }

        delegateActionHandler.setWrapped(menuConfig);

        if (hasView()) {
            getView().updateToolbar();
        }
    }

    ActionBarConfig getActionBarConfig() {
        if (delegateActionHandler == null) {
            delegateActionHandler = new DelegateActionHandler();
        }
        return ActionBarConfig.builder()
                .setTitle(R.string.title_playlists)
                .setMenuConfig(delegateActionHandler)
                .build();
    }

    class DelegateActionHandler implements ActionBarMenuHandler {

        ActionBarMenuHandler wrapped;

        void setWrapped(ActionBarMenuHandler wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public boolean onBuildMenu(MenuInflater menuInflater, Menu menu) {
            return wrapped != null && wrapped.onBuildMenu(menuInflater, menu);
        }

        @Override
        public boolean onMenuItemClicked(Context context, MenuItem menuItem) {
            return wrapped != null && wrapped.onMenuItemClicked(context, menuItem);
        }
    }
}
