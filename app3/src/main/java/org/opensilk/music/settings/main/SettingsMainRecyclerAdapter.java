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

package org.opensilk.music.settings.main;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.music.R;
import org.opensilk.music.settings.plugin.SettingsPluginRecyclerAdapter;
import org.opensilk.music.ui3.common.RecyclerAdapterItemClickDelegate;

import java.util.Arrays;

/**
 * Created by drew on 5/18/15.
 */
public class SettingsMainRecyclerAdapter extends RecyclerListAdapter<SettingsMainItem, SettingsMainRecyclerAdapter.ViewHolder> {

    RecyclerAdapterItemClickDelegate<ViewHolder> itemClickDelegate;

    public SettingsMainRecyclerAdapter() {
        super(Arrays.asList(SettingsMainItem.values()));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, R.layout.mtrl_list_item_oneline_icontext));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SettingsMainItem item = getItem(position);
        TextView tv = (TextView) holder.itemView;
        tv.setText(item.title);
        tv.setCompoundDrawablesWithIntrinsicBounds(item.iconRes, 0, 0, 0);
        if (itemClickDelegate != null) {
            itemClickDelegate.bindClickListener(holder, position);
        }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        if (itemClickDelegate != null) {
            itemClickDelegate.onViewRecycled(holder);
        }
    }

    void setClickListener(RecyclerAdapterItemClickDelegate.ItemClickListener l) {
        itemClickDelegate = new RecyclerAdapterItemClickDelegate<>(l);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements RecyclerAdapterItemClickDelegate.ClickableItem {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public View getContentView() {
            return itemView;
        }
    }
}
