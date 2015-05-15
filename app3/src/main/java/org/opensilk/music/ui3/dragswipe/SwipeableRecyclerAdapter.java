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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractSwipeableItemViewHolder;
import com.h6ah4i.android.widget.advrecyclerview.utils.RecyclerViewAdapterUtils;

import org.opensilk.common.ui.recycler.RecyclerListAdapter;

import java.util.List;

import timber.log.Timber;

/**
 * Created by drew on 5/15/15.
 */
public abstract class SwipeableRecyclerAdapter<T, VH extends SwipeableRecyclerAdapter.ViewHolder>
        extends RecyclerListAdapter<T, VH> implements SwipeableItemAdapter<VH> {

    public SwipeableRecyclerAdapter() {
        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    public SwipeableRecyclerAdapter(List<T> items) {
        super(items);
        // SwipeableItemAdapter requires stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    protected abstract void onItemRemoved(Context context, int position, T item);

    @Override
    public void onBindViewHolder(VH holder, int position) {

        // set background resource (target view ID: container)
        final int swipeState = holder.getSwipeStateFlags();

//        if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_IS_UPDATED) != 0) {
//            int bgResId;
//
//            if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_IS_ACTIVE) != 0) {
//                bgResId = R.drawable.bg_item_swiping_active_state;
//            } else if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_SWIPING) != 0) {
//                bgResId = R.drawable.bg_item_swiping_state;
//            } else {
//                bgResId = R.drawable.bg_item_normal_state;
//            }
//
//            holder.getSwipeableContainerView().setBackgroundResource(bgResId);
//        }

        // set swiping properties
        holder.setSwipeItemSlideAmount(0);
    }

    @Override
    public int onGetSwipeReactionType(VH holder, int position, int x, int y) {
        return RecyclerViewSwipeManager.REACTION_CAN_SWIPE_BOTH;
    }

    @Override
    public void onSetSwipeBackground(VH holder, int position, int type) {
//        int bgRes = 0;
//        switch (type) {
//            case RecyclerViewSwipeManager.DRAWABLE_SWIPE_NEUTRAL_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_neutral;
//                break;
//            case RecyclerViewSwipeManager.DRAWABLE_SWIPE_LEFT_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_left;
//                break;
//            case RecyclerViewSwipeManager.DRAWABLE_SWIPE_RIGHT_BACKGROUND:
//                bgRes = R.drawable.bg_swipe_item_right;
//                break;
//        }
//
//        holder.itemView.setBackgroundResource(bgRes);
    }

    @Override
    public int onSwipeItem(VH holder, int position, int result) {
        Timber.v("onSwipeItem(result = %d)", result);
        switch (result) {
            case RecyclerViewSwipeManager.RESULT_SWIPED_RIGHT:
            case RecyclerViewSwipeManager.RESULT_SWIPED_LEFT:
                return RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM;
            case RecyclerViewSwipeManager.RESULT_CANCELED:
            default:
                return RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_DEFAULT;
        }
    }

    @Override
    public void onPerformAfterSwipeReaction(VH holder, int position, int result, int reaction) {
        Timber.v("onPerformAfterSwipeReaction(position = %d, result = %d, reaction = %d)", position, result, reaction);
        if (reaction == RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM) {
            T item = removeItem(position);
            notifyItemRemoved(position);
            onItemRemoved(holder.itemView.getContext(), position, item);
        }
    }

    public abstract static class ViewHolder extends AbstractSwipeableItemViewHolder {
        public ViewHolder(View v) {
            super(v);
        }
    }
}
