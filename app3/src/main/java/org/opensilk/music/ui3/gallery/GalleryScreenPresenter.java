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

package org.opensilk.music.ui3.gallery;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;

import javax.inject.Inject;

import mortar.ViewPresenter;
import rx.functions.Func2;

/**
 * Created by drew on 4/20/15.
 */
@GalleryScreenScope
public class GalleryScreenPresenter extends ViewPresenter<GalleryScreenView> {

    final AppPreferences preferences;
    final GalleryScreen screen;
    final ToolbarOwner toolbarOwner;

    DelegateActionHandler delegateActionHandler;

    @Inject
    public GalleryScreenPresenter(
            AppPreferences preferences,
            GalleryScreen screen,
            ToolbarOwner toolbarOwner
    ) {
        this.preferences = preferences;
        this.screen = screen;
        this.toolbarOwner = toolbarOwner;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        // init acitonbar
        updateActionBarWithChildMenuConfig(null);
        // init pager
//        List<GalleryPage> galleryPages = preferences.getGalleryPages();
        int startPage = preferences.getInt(preferences.makePluginPrefKey(
                screen.libraryConfig, AppPreferences.GALLERY_START_PAGE), AppPreferences.DEFAULT_PAGE);
        getView().setup(screen.pages, startPage);
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (hasView()) {
            int pos = getView().mViewPager.getCurrentItem();
            preferences.putInt(preferences.makePluginPrefKey(
                    screen.libraryConfig, AppPreferences.GALLERY_START_PAGE), pos);
        }
    }

    void updateActionBarWithChildMenuConfig(ActionBarMenuConfig menuConfig) {
        if (delegateActionHandler == null) {
            delegateActionHandler = new DelegateActionHandler();
        }
        ActionBarMenuConfig.Builder builder = ActionBarMenuConfig.builder();

        builder.setActionHandler(delegateActionHandler);

        if (menuConfig != null) {
            delegateActionHandler.setDelegate(menuConfig.actionHandler);
            if (menuConfig.menus != null && menuConfig.menus.length > 0) {
                builder.withMenus(menuConfig.menus);
            }
            if (menuConfig.customMenus != null && menuConfig.customMenus.length > 0) {
                builder.withMenus(menuConfig.customMenus);
            }
        } else {
            delegateActionHandler.setDelegate(null);
        }

        toolbarOwner.setConfig(ActionBarConfig.builder()
                .setTitle(R.string.my_library)
                .setMenuConfig(builder.build())
                .build());
    }

    class DelegateActionHandler implements Func2<Context, Integer, Boolean> {

        Func2<Context, Integer, Boolean> delegate;

        void setDelegate(Func2<Context, Integer, Boolean> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Boolean call(Context context, Integer integer) {
            switch (integer) {
                default:
                    return delegate != null && hasView() && delegate.call(getView().getContext(), integer);
            }
        }
    }

}
