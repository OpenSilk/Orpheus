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
import android.view.ViewGroup;

import org.opensilk.common.ui.mortar.ActionBarMenuConfig;
import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.common.ui.mortar.MortarPagerAdapter;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleableRecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by drew on 4/20/15.
 */
class GalleryScreenViewAdapter extends MortarPagerAdapter<Screen, BundleableRecyclerView> {
    private Object mCurrentPrimaryItem;
    private GalleryScreenPresenter presenter;
    private List<GalleryPage> pages;

    GalleryScreenViewAdapter(Context context, GalleryScreenPresenter presenter, List<GalleryPage> pages) {
        super(context, extractScreens(pages));
        this.presenter = presenter;
        this.pages = pages;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (object != mCurrentPrimaryItem) {
            Page currentPage = (Page) object;
            BundleablePresenter childPresenter = (currentPage.view).getPresenter();
            ActionBarMenuHandler menuConfig = null;
            if (childPresenter != null) {
                menuConfig = childPresenter.getMenuConfig();
            }
            presenter.updateActionBarWithChildMenuConfig(menuConfig);
            mCurrentPrimaryItem = object;
        }
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return context.getString(pages.get(position).titleRes).toUpperCase(Locale.getDefault());
    }

    static List<Screen> extractScreens(List<GalleryPage> pages) {
        List<Screen> screens = new ArrayList<>(pages.size());
        for (GalleryPage p : pages) {
            screens.add(p.FACTORY.call());
        }
        return screens;
    }
}
