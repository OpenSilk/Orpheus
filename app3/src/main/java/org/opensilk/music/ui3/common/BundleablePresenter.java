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
import android.view.Menu;
import android.widget.PopupMenu;

import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.spi.Bundleable;

import mortar.ViewPresenter;

/**
 * Created by drew on 5/2/15.
 */
public abstract class BundleablePresenter extends ViewPresenter<BundleableRecyclerView> {
    public abstract void onItemClicked(Context context, Bundleable item);
    public abstract void onOverflowClicked(Context context, PopupMenu m, Bundleable item);
    public abstract boolean onOverflowActionClicked(Context context, OverflowAction action, Bundleable item);
    public abstract ArtworkRequestManager getRequestor();
}
