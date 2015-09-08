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
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.ui.recycler.DragSwipeAdapterWrapper;
import org.opensilk.music.R;
import org.opensilk.music.ui3.common.BundleableRecyclerAdapter;
import org.opensilk.music.ui3.common.UtilsCommon;

/**
 * Created by drew on 9/5/15.
 */
public class ProfileView2DragSwipePortrait extends ProfileView2Portrait {

    private ActionMode mActionMode;

    public ProfileView2DragSwipePortrait(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    @Override
    protected void initRecyclerView() {
        getListView().setHasFixedSize(true);
        DragSwipeAdapterWrapper<BundleableRecyclerAdapter.ViewHolder> wrapper =
                new DragSwipeAdapterWrapper<>(mAdapter, mAdapterCallback);
        getListView().setAdapter(wrapper);
    }

    @Override
    public void setupRecyclerView() {
        super.setupRecyclerView();
        mAdapter.setDragableList(true);
    }

    protected void startActionMode() {
        if (mActionMode == null) {
            mActionMode = UtilsCommon.findActivity(getContext()).startSupportActionMode(mActionModeCallback);
        }
    }

    final DragSwipeAdapterWrapper.Listener mAdapterCallback = new DragSwipeAdapterWrapper.Listener() {
        @Override
        public void onChange() {
            startActionMode();
        }
    };

    final ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
        boolean wasSaved = false;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.action_save, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_save:
                    wasSaved = true;
                    //TODO
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
            if (!wasSaved) {
                mPresenter.reload();
            }
        }
    };

}
