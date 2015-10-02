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

package org.opensilk.music.ui3.common;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.ui.mortar.ActionBarMenuHandler;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.library.LibraryConfig;

import rx.functions.Func2;

/**
 * Created by drew on 5/9/15.
 */
public abstract class ActionBarMenuBaseHandler implements Func2<Context, Integer, Boolean> {

    protected final LibraryConfig libraryConfig;
    protected final String sortOrderKey;
    protected final String layoutKey;
    protected final AppPreferences appPreferences;

    public ActionBarMenuBaseHandler(
            LibraryConfig libraryConfig,
            String sortOrderKey,
            String layoutKey,
            AppPreferences appPreferences
    ) {
        this.libraryConfig = libraryConfig;
        this.sortOrderKey = sortOrderKey;
        this.layoutKey = layoutKey;
        this.appPreferences = appPreferences;
    }

    protected void setNewSortOrder(BundleablePresenter presenter, String sortorder) {
        appPreferences.putString(appPreferences.makePluginPrefKey(libraryConfig, sortOrderKey), sortorder);
        presenter.getLoader().setSortOrder(sortorder);
        presenter.reload();
    }

    protected void updateLayout(BundleablePresenter presenter, String kind) {
        appPreferences.putString(appPreferences.makePluginPrefKey(libraryConfig, layoutKey), kind);
        presenter.setWantsGrid(StringUtils.equals(kind, AppPreferences.GRID));
        presenter.resetRecyclerView();
    }
}
