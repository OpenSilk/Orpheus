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

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.view.ViewGroup;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortarflow.MortarPagerAdapter;
import org.opensilk.music.ui2.core.android.ActionBarOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mortar.ViewPresenter;

/**
 * Created by drew on 4/20/15.
 */
class GalleryScreenAdapter extends MortarPagerAdapter<Screen, GalleryPageView> {
    private Object mCurrentPrimaryItem;
    private GalleryScreenPresenter presenter;

    GalleryScreenAdapter(Context context, GalleryScreenPresenter presenter, List<GalleryPage> pages) {
        super(context, extractScreens(pages));
        this.presenter = presenter;
    }

    static List<Screen> extractScreens(List<GalleryPage> pages) {
        List<Screen> sceens = new ArrayList<>(pages.size());
        for (GalleryPage p : pages) {
            sceens.add(p.screen);
        }
        return sceens;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (object != mCurrentPrimaryItem) {
            Page currentPage = (Page) object;
            ViewPresenter<GalleryPageView> childPresenter = (currentPage.view).getPresenter();
            ActionBarOwner.MenuConfig menuConfig = null;
            if (childPresenter != null && childPresenter instanceof HasOptionsMenu) {
                menuConfig = ((HasOptionsMenu) childPresenter).getMenuConfig();
            }
            presenter.updateActionBarWithChildMenuConfig(menuConfig);
            mCurrentPrimaryItem = object;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Screen screen = screens.get(position);
        int res = GalleryScreenView.getGalleryPageTitleResource(screen);
        return context.getString(res).toUpperCase(Locale.getDefault());
    }

}
