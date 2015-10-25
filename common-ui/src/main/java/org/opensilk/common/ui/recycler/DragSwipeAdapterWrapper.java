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

package org.opensilk.common.ui.recycler;

import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by drew on 9/5/15.
 */
public class DragSwipeAdapterWrapper<VH extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<VH> implements SimpleItemTouchHelperCallback.Listener {

    public interface Listener {
        void onChange();
    }

    protected final RecyclerListAdapter<?, VH> wrappedAdapter;
    protected final Listener listener;
    protected ItemTouchHelper itemTouchHelper;

    public DragSwipeAdapterWrapper(RecyclerListAdapter<?, VH> wrappedAdapter, Listener listener) {
        super();
        this.wrappedAdapter = wrappedAdapter;
        this.wrappedAdapter.registerAdapterDataObserver(observer);
        this.listener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        itemTouchHelper = new ItemTouchHelper(new SimpleItemTouchHelperCallback(this));
        itemTouchHelper.attachToRecyclerView(recyclerView);
        wrappedAdapter.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        itemTouchHelper = null;
        wrappedAdapter.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public boolean onItemMove(int fromPosition, int toPosition) {
        boolean success = wrappedAdapter.move(fromPosition, toPosition);
        if (success && listener != null) {
            listener.onChange();
        }
        return success;
    }

    @Override
    public void onItemDismiss(int position) {
        Object obj = wrappedAdapter.removeItem(position);
        if (obj != null && listener != null) {
            listener.onChange();
        }
    }

    @Override
    public VH onCreateViewHolder(ViewGroup viewGroup, int i) {
        return wrappedAdapter.onCreateViewHolder(viewGroup, i);
    }

    @Override
    public void onBindViewHolder(final VH viewHolder, int i) {
        wrappedAdapter.onBindViewHolder(viewHolder, i);
        // Start a drag whenever the handle view it touched
        if (viewHolder instanceof DragSwipeViewHolder) {
            DragSwipeViewHolder vh = (DragSwipeViewHolder) viewHolder;
            vh.getDragHandle().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_DOWN) {
                        itemTouchHelper.startDrag(viewHolder);
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return wrappedAdapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        return wrappedAdapter.getItemViewType(position);
    }

    final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyItemRangeRemoved(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            notifyItemRangeChanged(positionStart, itemCount, payload);
        }

    };
}
