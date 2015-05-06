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

package org.opensilk.music.ui3.profile;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.R;
import org.opensilk.music.ui3.common.BundleableRecyclerView;

/**
 * Created by drew on 5/6/15.
 */
public class ProfileLandscapeRecyclerView extends BundleableRecyclerView {

    public ProfileLandscapeRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void doInject() {
        ProfileComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
    }

    @Override
    protected RecyclerView.LayoutManager makeGridLayoutManager() {
        final int numCols = getContext().getResources().getInteger(R.integer.profile_grid_cols_horizontal);
        return new GridLayoutManager(getContext(), numCols, GridLayoutManager.VERTICAL, false);
    }
}
