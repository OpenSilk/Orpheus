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

package org.opensilk.music.ui3.nowplaying;

import android.content.Context;
import android.support.v7.internal.view.menu.MenuBuilder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.common.ui.recycler.DragSwipeAdapterWrapper;
import org.opensilk.music.R;
import org.opensilk.music.ui3.common.BundleableRecyclerAdapter;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

/**
 * Created by drew on 5/9/15.
 */
public class QueueScreenView extends RelativeLayout {

    @Inject QueueScreenPresenter mPresenter;
    @Inject QueueScreenViewAdapter mAdapter;

    @InjectView(R.id.queue_toolbar) Toolbar mToolbar;
    @InjectView(R.id.recyclerview) RecyclerView mList;

    public QueueScreenView(Context context, AttributeSet attrs) {
        super(context, attrs);
        QueueScreenComponent component = DaggerService.getDaggerComponent(getContext());
        component.inject(this);
    }

    @Override
    @DebugLog
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        mList.setHasFixedSize(true);
        mList.setLayoutManager(new LinearLayoutManager(getContext()));
        DragSwipeAdapterWrapper<QueueScreenViewAdapter.ViewHolder> wrapper =
                new DragSwipeAdapterWrapper<>(mAdapter, null);
        mList.setAdapter(wrapper);
        mPresenter.takeView(this);
    }

    @Override
    @DebugLog
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mAdapter.registerAdapterDataObserver(mObserver);
    }

    @Override
    @DebugLog
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mAdapter.unregisterAdapterDataObserver(mObserver);
        mPresenter.dropView(this);
    }

    public QueueScreenViewAdapter getAdapter() {
        return mAdapter;
    }

    public void setTitle(CharSequence title) {
        mToolbar.setTitle(title);
    }

    public void setSubTitle(CharSequence title) {
        mToolbar.setSubtitle(title);
    }

    final RecyclerView.AdapterDataObserver mObserver = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mPresenter.onItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mPresenter.onItemRemoved(positionStart);
        }
    };

}
