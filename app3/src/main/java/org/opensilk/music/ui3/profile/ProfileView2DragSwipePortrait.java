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
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.recycler.DragSwipeAdapterWrapper;
import org.opensilk.common.ui.recycler.DragSwipeViewHolder;
import org.opensilk.common.ui.recycler.SimpleItemTouchHelperCallback;
import org.opensilk.music.R;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleableRecyclerAdapter;
import org.opensilk.music.ui3.common.UtilsCommon;

import javax.inject.Inject;

/**
 * Created by drew on 9/5/15.
 */
public class ProfileView2DragSwipePortrait extends ProfileView2Portrait {

    boolean inSelectionMode;

    public ProfileView2DragSwipePortrait(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void inject() {
        ProfileComponentDragSwipe cmp = DaggerService.getDaggerComponent(getContext());
        cmp.inject(this);
    }

    @Override
    protected void initRecyclerView() {
        getListView().setHasFixedSize(true);
        DragSwipeAdapterWrapper<BundleableRecyclerAdapter.ViewHolder> wrapper =
                new DragSwipeAdapterWrapper<>(mAdapter, mMoveListener);
        getListView().setAdapter(wrapper);
    }

    @Override
    public void setupRecyclerView() {
        super.setupRecyclerView();
        mAdapter.setDragableList(true);
        mAdapter.setAllowSelectionMode(false);
    }

    final DragSwipeAdapterWrapper.Listener mMoveListener =
            new DragSwipeAdapterWrapper.Listener() {
                @Override
                public void onChange() {
                    if (!inSelectionMode) {
                        inSelectionMode = true;
                        mPresenter.onStartSelectionMode(mSelectionModeListener);
                    }
                }
    };

    final BundleableRecyclerAdapter.OnSelectionModeEnded mSelectionModeListener =
            new BundleableRecyclerAdapter.OnSelectionModeEnded() {
                @Override
                public void onEndSelectionMode() {
                    inSelectionMode = false;
                    mPresenter.reload();
                }
    };

}
