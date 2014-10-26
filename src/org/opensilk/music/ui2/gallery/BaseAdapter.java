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
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.opensilk.common.theme.TintDrawableWrapper;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.ui2.theme.Themer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 10/18/14.
 */
public abstract class BaseAdapter<T> extends RecyclerView.Adapter<BaseAdapter.ViewHolder> {

    protected final List<T> items;
    protected final ArtworkRequestManager artworkRequestor;

//    private LayoutInflater inflater;
    protected boolean mGridStyle = true;

    public interface ItemClickListener<T> {
        void OnItemClick(View view, T item);
    }

    protected ItemClickListener<T> itemClickListener;
    protected PopupMenu.OnMenuItemClickListener overflowClickListener;

    public BaseAdapter(ArtworkRequestManager artworkRequestor) {
        this.items = new ArrayList<>();
        this.artworkRequestor = artworkRequestor;
        setHasStableIds(true);
    }

    public BaseAdapter(List<T> items, ArtworkRequestManager artworkRequestor) {
        this.items = items;
        this.artworkRequestor = artworkRequestor;
        setHasStableIds(true);
    }

    public List<T> getItems() {
        return items;
    }

    public T getItem(int position) {
        return items.get(position);
    }

    public void addAll(Collection<T> items) {
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    public void add(T item) {
        items.add(item);
//        notifyItemInserted(items.indexOf(item));
        // bug in StaggeredGrid tries to arrayCopy items.size() + 1 and barfs
        notifyItemRangeInserted(items.indexOf(item), 0);
    }

    public void clear() {
        this.items.clear();
        notifyDataSetChanged();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void setItemClickListener(ItemClickListener<T> l) {
        this.itemClickListener = l;
    }

    public void setOverflowClickListener(PopupMenu.OnMenuItemClickListener l) {
        this.overflowClickListener = l;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Not caching this right now so i dont hold any context stuff
        //in the presenters
//        if (inflater == null) {
          LayoutInflater inflater = LayoutInflater.from(parent.getContext());
//        }
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
        return getItem(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        if (!mGridStyle) {
            return R.layout.gallery_list_item_artwork;
        }
        if (quadArtwork(position)) {
            return R.layout.gallery_grid_item_artwork4;
        } else if (dualArtwork(position)) {
            return R.layout.gallery_grid_item_artwork2;
        } else {
            return R.layout.gallery_grid_item_artwork;
        }
    }

    public void setGridStyle(boolean gridStyle) {
        mGridStyle = gridStyle;
    }

    protected boolean dualArtwork(int position) {
        return false;
    }

    protected boolean quadArtwork(int position) {
        return false;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.artwork_thumb) ImageView artwork;
        @InjectView(R.id.artwork_thumb2) @Optional ImageView artwork2;
        @InjectView(R.id.artwork_thumb3) @Optional ImageView artwork3;
        @InjectView(R.id.artwork_thumb4) @Optional ImageView artwork4;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

        final CompositeSubscription subscriptions;
        final int artNumber;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
            if (artwork4 != null) {
                artNumber = 4;
            } else if (artwork2 != null) {
                artNumber = 2;
            } else {
                artNumber = 1;
            }
//            Drawable d = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_menu_moreoverflow_mtrl_alpha);
//            overflow.setImageDrawable(new TintDrawableWrapper(d, Themer.getDefaultColorStateList(itemView.getContext())));
        }

        public void reset() {
//            Timber.v("Reset title=%s", title.getText());
            if (artwork != null) artwork.setImageBitmap(null);
            if (artwork2 != null) artwork2.setImageBitmap(null);
            if (artwork3 != null) artwork3.setImageBitmap(null);
            if (artwork4 != null) artwork4.setImageBitmap(null);
            subscriptions.clear();
        }

    }
}
