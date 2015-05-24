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

package org.opensilk.music.settings.plugin;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.ui3.common.RecyclerAdapterItemClickDelegate;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by drew on 5/23/15.
 */
public class SettingsPluginRecyclerAdapter extends RecyclerListAdapter<LibraryProviderInfo, SettingsPluginRecyclerAdapter.ViewHolder> {

    final RecyclerAdapterItemClickDelegate<ViewHolder> itemClickDelegate;
    final SettingsPluginPresenter presenter;

    @Inject
    public SettingsPluginRecyclerAdapter(SettingsPluginPresenter presenter) {
        this.presenter = presenter;
        itemClickDelegate = new RecyclerAdapterItemClickDelegate<ViewHolder>(presenter);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, R.layout.settings_plugin_item));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        LibraryProviderInfo item = getItem(position);
        Resources res = holder.itemView.getResources();
        Drawable d = item.icon;
        if (d == null) {
            d = res.getDrawable(R.drawable.ic_extension_grey600_24dp);
        }
        int bounds = (int) (24 * res.getDisplayMetrics().density);
        d.setBounds(0, 0, bounds, bounds);
        holder.icon.setImageDrawable(d);
        holder.text.setText(item.title);
        holder.subtext.setText(item.description);
        holder.checkBox.setChecked(item.isActive);
        itemClickDelegate.bindClickListener(holder, position);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        itemClickDelegate.onViewRecycled(holder);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder
            implements RecyclerAdapterItemClickDelegate.ClickableItem {
        @InjectView(R.id.icon) ImageView icon;
        @InjectView(R.id.line1) TextView text;
        @InjectView(R.id.line2) TextView subtext;
        @InjectView(R.id.checkbox) CheckBox checkBox;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }

        @Override
        public View getContentView() {
            return itemView;
        }
    }
}
