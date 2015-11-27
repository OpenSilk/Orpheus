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

import org.opensilk.common.ui.recycler.ItemClickSupport;
import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.music.R;

import java.util.Arrays;

/**
 * Created by drew on 5/18/15.
 */
public class SettingsMainRecyclerAdapter extends RecyclerListAdapter<SettingsMainItem,
        SettingsMainRecyclerAdapter.ViewHolder> {

    private ItemClickSupport.OnItemClickListener clickListener;

    public SettingsMainRecyclerAdapter(ItemClickSupport.OnItemClickListener l) {
        super(Arrays.asList(SettingsMainItem.values()));
        this.clickListener = l;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        ItemClickSupport.addTo(recyclerView)
                .setOnItemClickListener(clickListener);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        ItemClickSupport.removeFrom(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, R.layout.mtrl_list_item_oneline_text));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SettingsMainItem item = getItem(position);
        TextView tv = (TextView) holder.itemView;
        tv.setText(item.title);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
