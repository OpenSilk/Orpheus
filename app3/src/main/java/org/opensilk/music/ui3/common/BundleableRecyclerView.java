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
import android.util.AttributeSet;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.recycler.RecyclerListFrame;

import javax.inject.Inject;

/**
 * Created by drew on 5/2/15.
 */
public class BundleableRecyclerView extends RecyclerListFrame {

    @Inject BundleablePresenter mPresenter;
    @Inject BundleableRecyclerAdapter mAdapter;

    public BundleableRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        DaggerService.<BundleableComponent>getDaggerComponent(context).inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        getListView().setHasFixedSize(true);
        getListView().setAdapter(mAdapter);
        mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
    }

    public BundleableRecyclerAdapter getAdapter() {
        return mAdapter;
    }
}
