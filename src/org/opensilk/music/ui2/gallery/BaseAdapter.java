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

package org.opensilk.music.ui2.gallery;

import android.graphics.drawable.Drawable;
import android.support.v7.internal.widget.TintManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import hugo.weaving.DebugLog;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 10/18/14.
 */
public abstract class BaseAdapter<T> extends RecyclerView.Adapter<BaseAdapter.ViewHolder> {

    private LayoutInflater inflater;
    protected final List<T> items;

    public BaseAdapter() {
        this.items = new ArrayList<>();
    }

    public BaseAdapter(List<T> items) {
        this.items = items;
    }

    public List<T> getItems() {
        return items;
    }

    public T getItem(int position) {
        return items.get(position);
    }

    public void addAll(Collection<T> items) {
        items.addAll(items);
        notifyDataSetChanged();
    }

    public void add(T item) {
        items.add(item);
        notifyItemInserted(items.size()-1);
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (inflater == null) {
            inflater = LayoutInflater.from(parent.getContext());
        }
        View view = inflater.inflate(viewType, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.reset();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemViewType(int position) {
        if (quadArtwork(position)) {
            return R.layout.gallery_grid_item_artwork;
        } else if (dualArtwork(position)) {
            return R.layout.gallery_grid_item_artwork;
        } else {
            return R.layout.gallery_grid_item_artwork;
        }
    }

    protected boolean dualArtwork(int position) {
        return false;
    }

    protected boolean quadArtwork(int position) {
        return false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.artwork_thumb) ArtworkImageView artwork;
        @InjectView(R.id.artwork_thumb2) @Optional ArtworkImageView artwork2;
        @InjectView(R.id.artwork_thumb3) @Optional ArtworkImageView artwork3;
        @InjectView(R.id.artwork_thumb4) @Optional ArtworkImageView artwork4;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

        CompositeSubscription subscriptions;
        int artNumber;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
        }

        public void loadArtwork(ArtInfo artInfo) {
            switch (++artNumber) {
                case 1:
                    if (artwork != null) ArtworkManager.loadImage(artInfo, artwork);
                    break;
                case 2:
                    if (artwork2 != null) ArtworkManager.loadImage(artInfo, artwork2);
                    break;
                case 3:
                    if (artwork3 != null) ArtworkManager.loadImage(artInfo, artwork3);
                    break;
                case 4:
                    if (artwork4 != null) ArtworkManager.loadImage(artInfo, artwork4);
                    break;
            }
        }

//        @DebugLog
        public void reset() {
            if (artwork != null) artwork.cancelRequest();
            if (artwork2 != null) artwork2.cancelRequest();
            if (artwork3 != null) artwork3.cancelRequest();
            if (artwork4 != null) artwork4.cancelRequest();
            subscriptions.unsubscribe();
            subscriptions.clear();
            artNumber=0;
        }

    }
}
