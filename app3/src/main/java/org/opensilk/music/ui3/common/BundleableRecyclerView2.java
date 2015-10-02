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

/**
 * Created by drew on 9/3/15.
 */
public interface BundleableRecyclerView2 {
    Context getContext();
    void setLoading(boolean loading);
    void setupRecyclerView();
    void setListShown(boolean show, boolean animate);
    void setListEmpty(boolean show, boolean animate);
    void setEmptyText(int resId);
    BundleableRecyclerAdapter getAdapter();
    void notifyAdapterResetIncoming();
}
