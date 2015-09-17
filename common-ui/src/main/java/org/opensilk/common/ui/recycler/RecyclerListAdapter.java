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

import android.support.annotation.LayoutRes;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by drew on 11/20/14.
 */
public abstract class RecyclerListAdapter<T, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    private final List<T> items;

    public RecyclerListAdapter() {
        this(new ArrayList<T>(100));
    }

    public RecyclerListAdapter(List<T> items) {
        this.items = items;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<T> getItems() {
        return items;
    }

    public T getItem(int pos) {
        return items.get(pos);
    }

    public boolean addAll(Collection<? extends T> collection) {
        int start = items.size();
        if (items.addAll(collection)) {
            notifyItemRangeInserted(start, collection.size());
            return true;
        }
        return false;
    }

    public boolean addAll(int pos, Collection<? extends T> collection) {
        if (items.addAll(pos, collection)) {
            notifyItemRangeInserted(pos, collection.size());
            return true;
        }
        return false;
    }

    public boolean replaceAll(Collection<? extends T> collection) {
        items.clear();
        if (items.addAll(collection)) {
            notifyDataSetChanged();
            return true;
        }
        return false;
    }

    public boolean addItem(T item) {
        if (items.add(item)) {
            //notifyItemInserted(items.indexOf(item));
            // bug in StaggeredGrid tries to arrayCopy items.size() + 1 and barfs
            notifyItemRangeInserted(items.indexOf(item), 0);
            return true;
        }
        return false;
    }

    public void addItem(int pos, T item) {
        items.add(pos, item);
        notifyItemInserted(pos);
    }

    public boolean removeItem(T item) {
        int pos = items.indexOf(item);
        if (items.remove(item)) {
            notifyItemRemoved(pos);
            return true;
        }
        return false;
    }

    public T removeItem(int pos) {
        T item = items.remove(pos);
        notifyItemRemoved(pos);
        return item;
    }

    public int indexOf(T item) {
        return items.indexOf(item);
    }

    public boolean swap(int pos1, int pos2) {
        try {
            Collections.swap(items, pos1, pos2);
            notifyItemMoved(pos1, pos2);
            return true;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    public boolean swap(T item1, T item2) {
        return swap(indexOf(item1), indexOf(item2));
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    //Convenience function
    protected static View inflate(ViewGroup parent, @LayoutRes int id) {
        return LayoutInflater.from(parent.getContext()).inflate(id, parent, false);
    }

}
