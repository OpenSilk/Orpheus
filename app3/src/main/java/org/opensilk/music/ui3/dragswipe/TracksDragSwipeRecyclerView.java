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

package org.opensilk.music.ui3.dragswipe;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.recycler.RecyclerListFrame;
import org.opensilk.music.R;
import org.opensilk.music.ui3.common.BundleableComponent;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.BundleableRecyclerAdapter;
import org.opensilk.music.ui3.common.BundleableRecyclerView;
import org.opensilk.music.ui3.common.UtilsCommon;

import javax.inject.Inject;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 5/13/15.
 */
public class TracksDragSwipeRecyclerView extends RecyclerListFrame {

    @Inject protected TracksDragSwipePresenter mPresenter;
    @Inject protected TracksDragSwipeRecyclerAdapter mAdapter;

    private ActionMode mActionMode;

    public TracksDragSwipeRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        doInject();
    }

    protected void doInject() {
        TracksDragSwipeComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initRecyclerView();
        mPresenter.takeView(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPresenter.dropView(this);
        if (mActionMode != null) {
            mActionMode.finish();
        }
    }

    protected void initRecyclerView() {
        getListView().setHasFixedSize(true);
        ((DragSwipeRecyclerView) getListView()).setup(mAdapter);
    }

    protected void setupRecyclerView() {
        getListView().getRecycledViewPool().clear();
        getListView().setLayoutManager(getLayoutManager());
    }

    protected void notifyAdapterResetIncoming() {

    }

    protected void startActionMode() {
        if (mActionMode == null) {
            mActionMode = UtilsCommon.findActivity(getContext()).startSupportActionMode(mActionModeCallback);
        }
    }

    protected RecyclerView.LayoutManager getLayoutManager() {
        return makeListLayoutManager();
    }

    protected RecyclerView.LayoutManager makeListLayoutManager() {
        return new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
    }

    public TracksDragSwipeRecyclerAdapter getAdapter() {
        return mAdapter;
    }

    public TracksDragSwipePresenter getPresenter() {
        return mPresenter;
    }

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
