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
import android.support.v4.view.ViewCompat;
import android.view.View;

import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange;
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.RecyclerViewSwipeManager;
import com.h6ah4i.android.widget.advrecyclerview.swipeable.SwipeableItemAdapter;
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableSwipeableItemViewHolder;

import org.opensilk.common.ui.recycler.RecyclerListAdapter;

import java.util.List;

import timber.log.Timber;

/**
 * Created by drew on 5/13/15.
 */
public abstract class DragSwipeRecyclerAdapter<T, VH extends DragSwipeRecyclerAdapter.ViewHolder>
        extends RecyclerListAdapter<T, VH> implements DraggableItemAdapter<VH>, SwipeableItemAdapter<VH> {

    public DragSwipeRecyclerAdapter() {
        super();
        // DraggableItemAdapter and SwipeableItemAdapter require stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    public DragSwipeRecyclerAdapter(List<T> items) {
        super(items);
        // DraggableItemAdapter and SwipeableItemAdapter require stable ID, and also
        // have to implement the getItemId() method appropriately.
        setHasStableIds(true);
    }

    protected abstract void onItemRemoved(Context context, int position, T item);

    @Override
    public void onBindViewHolder(VH holder, int position) {

        // set background resource (target view ID: container)
        final int dragState = holder.getDragStateFlags();
        final int swipeState = holder.getSwipeStateFlags();

        if (((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_UPDATED) != 0) ||
                ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_IS_UPDATED) != 0)) {
            int bgResId;

//            if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_IS_ACTIVE) != 0) {
//                bgResId = R.drawable.bg_item_dragging_active_state;
//            } else if ((dragState & RecyclerViewDragDropManager.STATE_FLAG_DRAGGING) != 0) {
//                bgResId = R.drawable.bg_item_dragging_state;
//            } else if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_IS_ACTIVE) != 0) {
//                bgResId = R.drawable.bg_item_swiping_active_state;
//            } else if ((swipeState & RecyclerViewSwipeManager.STATE_FLAG_SWIPING) != 0) {
//                bgResId = R.drawable.bg_item_swiping_state;
//            } else {
//                bgResId = R.drawable.bg_item_normal_state;
//            }

//            holder.getContainerView().setBackgroundResource(bgResId);
        }

        // set swiping properties
        holder.setSwipeItemSlideAmount(0);
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        Timber.v("onMoveItem(fromPosition = %d , toPosition = %d)", fromPosition, toPosition);
        if (fromPosition == toPosition) {
            return;
        }
        T item = removeItem(fromPosition);
        addItem(toPosition, item);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public boolean onCheckCanStartDrag(VH holder, int position, int x, int y) {
        // x, y --- relative from the itemView's top-left
        final View containerView = holder.getSwipeableContainerView();
        final View dragHandleView = holder.getDragHandle();

        final int offsetX = containerView.getLeft() + (int) (ViewCompat.getTranslationX(containerView) + 0.5f);
        final int offsetY = containerView.getTop() + (int) (ViewCompat.getTranslationY(containerView) + 0.5f);

        return hitTest(dragHandleView, x - offsetX, y - offsetY);
    }

    @Override
    public ItemDraggableRange onGetItemDraggableRange(VH holder, int position) {
        // no drag-sortable range specified
        return null;
    }

    @Override
    public int onGetSwipeReactionType(VH holder, int position, int x, int y) {
        if (onCheckCanStartDrag(holder, position, x, y)) {
            return RecyclerViewSwipeManager.REACTION_CAN_NOT_SWIPE_BOTH;
        } else {
            return RecyclerViewSwipeManager.REACTION_CAN_SWIPE_BOTH;
        }
    }

    @Override
    public void onSetSwipeBackground(VH holder, int position, int type) {
        int bgRes = 0;
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
        Timber.v("onPerformAfterSwipeReaction(result = %d, reaction = %d)", result, reaction);
        if (reaction == RecyclerViewSwipeManager.AFTER_SWIPE_REACTION_REMOVE_ITEM) {
            T item = removeItem(position);
            notifyItemRemoved(position);
            onItemRemoved(holder.itemView.getContext(), position, item);
        }
    }

    public static boolean hitTest(View v, int x, int y) {
        final int tx = (int) (ViewCompat.getTranslationX(v) + 0.5f);
        final int ty = (int) (ViewCompat.getTranslationY(v) + 0.5f);
        final int left = v.getLeft() + tx;
        final int right = v.getRight() + tx;
        final int top = v.getTop() + ty;
        final int bottom = v.getBottom() + ty;
        return (x >= left) && (x <= right) && (y >= top) && (y <= bottom);
    }

    public abstract static class ViewHolder extends AbstractDraggableSwipeableItemViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
        public abstract View getDragHandle();
    }
}
