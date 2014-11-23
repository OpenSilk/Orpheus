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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.opensilk.common.content.RecyclerListAdapter;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.widgets.GridTileDescription;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 10/18/14.
 */
public abstract class BaseAdapter<T> extends RecyclerListAdapter<T, BaseAdapter.ViewHolder> {

    protected final BasePresenter<T> presenter;
    protected final ArtworkRequestManager artworkRequestor;

    private LayoutInflater inflater;
    protected boolean mGridStyle = true;

    public BaseAdapter(BasePresenter<T> presenter, ArtworkRequestManager artworkRequestor) {
        this(new ArrayList<T>(), presenter, artworkRequestor);
    }

    public BaseAdapter(List<T> items, BasePresenter<T> presenter, ArtworkRequestManager artworkRequestor) {
        super(items);
        this.presenter = presenter;
        this.artworkRequestor = artworkRequestor;
        setHasStableIds(true);
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
    public void onBindViewHolder(final ViewHolder holder, int i) {
        final T item = getItem(i);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.onItemClicked(holder, item);
            }
        });
        holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu m = new PopupMenu(v.getContext(), v);
                presenter.onCreateOverflowMenu(m, item);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem i) {
                        try {
                            OverflowAction a = OverflowAction.valueOf(i.getItemId());
                            return presenter.onOverflowItemClicked(a, item);
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                });
                m.show();
            }
        });
        onBindViewHolder(holder, item);
    }

    protected abstract void onBindViewHolder(ViewHolder holder, T item);

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.reset();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public int getItemViewType(int position) {
        if (!mGridStyle) {
            return R.layout.gallery_list_item_artwork;
        } else if (multiArtwork(position)) {
            return R.layout.gallery_grid_item_artwork4;
        } else {
            return R.layout.gallery_grid_item_artwork;
        }
    }

    public void setGridStyle(boolean gridStyle) {
        mGridStyle = gridStyle;
    }

    protected boolean multiArtwork(int position) {
        return false;
    }

    static CompositeSubscription loadMultiArtwork(ArtworkRequestManager requestor,
                                                  CompositeSubscription cs,
                                                  long[] albumIds,
                                                  AnimatedImageView artwork,
                                                  AnimatedImageView artwork2,
                                                  AnimatedImageView artwork3,
                                                  AnimatedImageView artwork4) {
        final int num = albumIds.length;
        if (artwork != null) {
            if (num >= 1) {
                cs.add(requestor.newAlbumRequest(artwork, null, albumIds[0], ArtworkType.THUMBNAIL));
            } else {
                artwork.setDefaultImage();
            }
        }
        if (artwork2 != null) {
            if (num >= 2) {
                cs.add(requestor.newAlbumRequest(artwork2, null, albumIds[1], ArtworkType.THUMBNAIL));
            } else {
                // never get here
                artwork2.setDefaultImage();
            }
        }
        if (artwork3 != null) {
            if (num >= 3) {
                cs.add(requestor.newAlbumRequest(artwork3, null, albumIds[2], ArtworkType.THUMBNAIL));
            } else if (num >= 2) {
                //put the second image here, first image will be put in 4th spot to crisscross
                cs.add(requestor.newAlbumRequest(artwork3, null, albumIds[1], ArtworkType.THUMBNAIL));
            } else {
                // never get here
                artwork3.setDefaultImage();
            }
        }
        if (artwork4 != null) {
            if (num >= 4) {
                cs.add(requestor.newAlbumRequest(artwork4, null, albumIds[3], ArtworkType.THUMBNAIL));
            } else if (num >= 2) {
                //3 -> loopback, 2 -> put the first image here for crisscross
                cs.add(requestor.newAlbumRequest(artwork4, null, albumIds[0], ArtworkType.THUMBNAIL));
            } else {
                //never get here
                artwork4.setDefaultImage();
            }
        }
        return cs;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.artwork_thumb) public AnimatedImageView artwork;
        @InjectView(R.id.artwork_thumb2) @Optional public AnimatedImageView artwork2;
        @InjectView(R.id.artwork_thumb3) @Optional public AnimatedImageView artwork3;
        @InjectView(R.id.artwork_thumb4) @Optional public AnimatedImageView artwork4;
        @InjectView(R.id.grid_description) @Optional GridTileDescription descriptionContainer;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

        final CompositeSubscription subscriptions;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
        }

        public void reset() {
//            Timber.v("Reset title=%s", title.getText());
            if (artwork != null) artwork.setImageBitmap(null);
            if (artwork2 != null) artwork2.setImageBitmap(null);
            if (artwork3 != null) artwork3.setImageBitmap(null);
            if (artwork4 != null) artwork4.setImageBitmap(null);
            if (descriptionContainer != null) descriptionContainer.resetBackground();
            subscriptions.clear();
        }

    }
}
