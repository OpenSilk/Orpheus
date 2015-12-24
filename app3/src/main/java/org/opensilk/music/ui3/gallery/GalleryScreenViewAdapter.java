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
import android.view.ViewGroup;

import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.common.ui.mortar.MortarPagerAdapter;
import org.opensilk.common.ui.mortar.Screen;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleableRecyclerList;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by drew on 4/20/15.
 */
class GalleryScreenViewAdapter extends MortarPagerAdapter<GalleryPageScreen, BundleableRecyclerList> {
    private Object mCurrentPrimaryItem;
    private GalleryScreenPresenter presenter;

    GalleryScreenViewAdapter(Context context, GalleryScreenPresenter presenter, List<GalleryPageScreen> screens) {
        super(context, screens);
        this.presenter = presenter;
    }

    @Override @SuppressWarnings("unchecked")
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (object != mCurrentPrimaryItem) {
            Page<Screen, BundleableRecyclerList> currentPage = (Page<Screen, BundleableRecyclerList>) object;
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
        return context.getString(screens.get(position).titleResource).toUpperCase(Locale.getDefault());
    }
}
