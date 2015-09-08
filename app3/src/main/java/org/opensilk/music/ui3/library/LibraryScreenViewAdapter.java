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

package org.opensilk.music.ui3.library;

import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryProviderInfo;

import javax.inject.Inject;

/**
 * Created by drew on 9/6/15.
 */
public class LibraryScreenViewAdapter extends RecyclerListAdapter<LibraryProviderInfo, LibraryScreenViewAdapter.ViewHolder> {

    final LibraryScreenPresenter presenter;

    @Inject
    public LibraryScreenViewAdapter(
            LibraryScreenPresenter presenter
    ) {
        this.presenter = presenter;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, R.layout.mtrl_list_item_oneline_icontext));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final LibraryProviderInfo item = getItem(position);
        TextView tv = (TextView) holder.itemView;
        tv.setText(item.title);
        Drawable d = item.icon;
        item.icon = null; //free plugin reference
        if (d == null) {
            d = tv.getResources().getDrawable(R.drawable.ic_extension_grey600_24dp);
        }
        int bounds = (int) (24 * tv.getResources().getDisplayMetrics().density);
        d.setBounds(0, 0, bounds, bounds);
        tv.setCompoundDrawables(d, null, null, null);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onItemClick(item);
            }
        });
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.itemView.setOnClickListener(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
