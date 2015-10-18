/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by drew on 11/23/14.
 */
public class HeaderRecyclerAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerListAdapter<View, RecyclerView.ViewHolder> {

    protected final RecyclerListAdapter<?, VH> wrappedAdapter;

    public HeaderRecyclerAdapter(RecyclerListAdapter<?, VH> wrappedAdapter) {
        super();
        this.wrappedAdapter = wrappedAdapter;
        this.wrappedAdapter.registerAdapterDataObserver(observer);
    }

    public void addHeader(View v) {
        addItem(v);
    }

    public int getNumHeaders() {
        return getItems().size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        wrappedAdapter.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        wrappedAdapter.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        if (i < 0) {
            return new RecyclerView.ViewHolder(getItem((i + 1) * -1)) {};
        }
        return wrappedAdapter.onCreateViewHolder(viewGroup, i);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int i) {
        if (i < getNumHeaders()) {
            return;
        }
        wrappedAdapter.onBindViewHolder((VH)viewHolder, i - getNumHeaders());
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + wrappedAdapter.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (position < getNumHeaders()) {
            return (position * -1) - 1;
        }
        return wrappedAdapter.getItemViewType(position - getNumHeaders());
    }

    final RecyclerView.AdapterDataObserver observer = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            notifyItemRangeChanged(positionStart + getNumHeaders(), itemCount);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart + getNumHeaders(), itemCount);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            notifyItemRangeRemoved(positionStart + getNumHeaders(), itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            notifyItemMoved(fromPosition + getNumHeaders(), toPosition + getNumHeaders());
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
            notifyItemRangeChanged(positionStart, itemCount, payload);
        }
    };
}
