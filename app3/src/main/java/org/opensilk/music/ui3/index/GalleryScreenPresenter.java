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

package org.opensilk.music.ui3.index;

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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import mortar.ViewPresenter;

/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class GalleryScreenPresenter extends ViewPresenter<GalleryScreenView> {

    final AppPreferences preferences;
    final GalleryScreen screen;
    final ActionModePresenter actionModePresenter;

    DelegateActionHandler delegateActionHandler;

    @Inject
    public GalleryScreenPresenter(
            AppPreferences preferences,
            GalleryScreen screen,
            ActionModePresenter actionModePresenter
    ) {
        this.preferences = preferences;
        this.screen = screen;
        this.actionModePresenter = actionModePresenter;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        // init pager
//        List<GalleryPage> galleryPages = preferences.getGalleryPages();
        List<GalleryPage> galleryPages = Arrays.asList(GalleryPage.values());
        int startPage = preferences.getInt(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                AppPreferences.GALLERY_START_PAGE), AppPreferences.DEFAULT_PAGE);
        getView().setup(galleryPages, startPage);
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (hasView()) {
            int pos = getView().mViewPager.getCurrentItem();
            preferences.putInt(preferences.makePrefKey(AppPreferences.KEY_INDEX,
                    AppPreferences.GALLERY_START_PAGE), pos);
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
                .setTitle(R.string.my_library)
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
