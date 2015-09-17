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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.music.R;
import org.opensilk.music.library.LibraryProviderInfo;
import org.opensilk.music.model.spi.Bundleable;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;

/**
 * Created by drew on 9/6/15.
 */
public class LibraryScreenViewAdapter extends RecyclerListAdapter<Bundleable, LibraryScreenViewAdapter.ViewHolder> {

    final LibraryScreenPresenter presenter;

    @Inject
    public LibraryScreenViewAdapter(
            LibraryScreenPresenter presenter
    ) {
        this.presenter = presenter;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, viewType));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        final Bundleable b = getItem(position);

        if (b instanceof ProviderInfoBundleable) {
            final ProviderInfoBundleable item = (ProviderInfoBundleable)b;
            holder.line1.setText(item.getInfo().getTitle());
            holder.line2.setText(item.getInfo().getDescription());
            Drawable d = item.getIcon();
            if (d == null) {
                ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.ic_extension_grey600_24dp);
            }
//            int bounds = (int) (24 * holder.itemView.getResources().getDisplayMetrics().density);
//            d.setBounds(0, 0, bounds, bounds);
            holder.avatar.setImageDrawable(d);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    presenter.onItemClick(item.getInfo());
                }
            });
            holder.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO
                }
            });
            holder.progress.setVisibility(item.isLoading() ? View.VISIBLE : View.INVISIBLE);
            holder.loginBtn.setVisibility(item.isNeedsLogin() ? View.VISIBLE : View.INVISIBLE);
            holder.loginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        } else {
            TextView tv = (TextView) holder.itemView;
            tv.setText(b.getDisplayName());
            tv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //TODO
                }
            });
        }

    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.itemView.setOnClickListener(null);
    }

    @Override
    public int getItemViewType(int position) {
        Bundleable b = getItem(position);
        if (b instanceof ProviderInfoBundleable) {
            return R.layout.library_provider_item_view;
        } else {
            return R.layout.mtrl_list_item_oneline_text;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.line1) @Optional TextView line1;
        @InjectView(R.id.line2) @Optional TextView line2;
        @InjectView(R.id.avatar) @Optional ImageView avatar;
        @InjectView(R.id.loading_progress) @Optional ProgressBar progress;
        @InjectView(R.id.btn_login) @Optional Button loginBtn;
        @InjectView(R.id.tile_overflow) @Optional View overflow;
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            reset();
        }

        void reset() {
            if (progress != null) progress.setVisibility(View.VISIBLE);
            if (loginBtn != null) loginBtn.setVisibility(View.GONE);
        }
    }
}
