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
import android.net.Uri;
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
import org.opensilk.music.library.gallery.GalleryClient;

import java.util.ArrayList;
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
        List<GalleryPageScreen> screens = buildPages();
        int startPage = preferences.getGalleryStartPage(screen.authority);
        getView().setup(screens, startPage);
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (hasView()) {
            int pos = getView().mViewPager.getCurrentItem();
            preferences.putInt(preferences.galleryStartPageKey(screen.authority), pos);
        }
    }

    List<GalleryPageScreen> buildPages() {
        GalleryClient client = GalleryClient.acquire(getView().getContext(), screen.authority);
        try {
            List<GalleryPage> galleryPages = Arrays.asList(GalleryPage.values());
            List<GalleryPageScreen> screens = new ArrayList<>(galleryPages.size());
            for (GalleryPage page : galleryPages) {
                Uri uri = null;
                switch (page) {
                    case ALBUM:
                        uri = client.getAlbumsUri();
                        break;
                    case ARTIST:
                        uri = client.getArtistsUri();
                        break;
                    case GENRE:
                        uri = client.getGenresUri();
                        break;
                    case SONG:
                        uri = client.getTracksUri();
                        break;
                    case FOLDER:
                        uri = client.getIndexedFoldersUri();
                        break;
                }
                if (uri != null) {
                    screens.add(page.FACTORY.call(uri));
                }
            }
            return screens;
        } finally {
            client.release();
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
                .setTitle(screen.titleResource)
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
            menuInflater.inflate(R.menu.refresh, menu);
            if (wrapped != null) {
                wrapped.onBuildMenu(menuInflater, menu);
            }
            return true;
        }

        @Override
        public boolean onMenuItemClicked(Context context, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.refresh:
                    context.getContentResolver().notifyChange(IndexUris.call(screen.authority), null);
                    return true;
                default:
                    return wrapped != null && wrapped.onMenuItemClicked(context, menuItem);
            }
        }
    }

}
