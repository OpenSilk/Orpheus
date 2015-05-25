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

package org.opensilk.music.ui3.dragswipe;

import android.content.Context;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.opensilk.common.core.rx.SimpleObserver;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.common.ui.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui.widget.PlayingIndicator;

import java.util.Map;
import java.util.WeakHashMap;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observer;
import rx.Subscription;
import rx.android.events.OnClickEvent;
import rx.android.observables.ViewObservable;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 5/13/15.
 */
public abstract class BaseDragSwipeRecyclerAdapter<T> extends DragSwipeRecyclerAdapter<T, BaseDragSwipeRecyclerAdapter.ViewHolder> {

    final Map<View, SubCont> itemClickSubscriptions = new WeakHashMap<>();
    final Map<View, SubCont> overflowClickSubscriptions = new WeakHashMap<>();

    public BaseDragSwipeRecyclerAdapter() {
        super();
    }

    protected abstract void onItemClicked(Context context, T item);
    protected abstract void onOverflowClicked(Context context, PopupMenu menu, T item);
    protected abstract boolean onOverflowActionClicked(Context context, OverflowAction action, T item);

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, R.layout.gallery_list_item_dragsort));
    }

    protected void bindClickListeners(ViewHolder holder, int position) {
        itemClickSubscriptions.put(holder.itemView,
                SubCont.ni(position, ViewObservable.clicks(holder.itemView).subscribe(itemClickObserver)));
        overflowClickSubscriptions.put(holder.overflow,
                SubCont.ni(position, ViewObservable.clicks(holder.overflow).subscribe(overflowClickObserver)));
    }

    protected void setLetterTileDrawable(ViewHolder holder, String text) {
        Resources resources = holder.itemView.getResources();
        LetterTileDrawable drawable = LetterTileDrawable.fromText(resources, text);
        holder.artwork.setImageDrawable(drawable);
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        SubCont c = itemClickSubscriptions.remove(holder.itemView);
        if (c != null) {
            c.sub.unsubscribe();
        }
        c = overflowClickSubscriptions.remove(holder.overflow);
        if (c != null) {
            c.sub.unsubscribe();
        }
        holder.reset();
    }

    final Observer<OnClickEvent> itemClickObserver = new SimpleObserver<OnClickEvent>() {
        @Override
        public void onNext(OnClickEvent onClickEvent) {
            SubCont c = itemClickSubscriptions.get(onClickEvent.view);
            if (c != null) {
                Context context = onClickEvent.view.getContext();
                onItemClicked(context, getItem(c.pos));
            }
        }
    };

    final Observer<OnClickEvent> overflowClickObserver = new SimpleObserver<OnClickEvent>() {
        @Override
        public void onNext(OnClickEvent onClickEvent) {
            SubCont c = overflowClickSubscriptions.get(onClickEvent.view);
            if (c != null) {
                final Context context = onClickEvent.view.getContext();
                final T t = getItem(c.pos);
                PopupMenu m = new PopupMenu(context, onClickEvent.view);
                onOverflowClicked(context, m, t);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            OverflowAction action = OverflowAction.valueOf(item.getItemId());
                            return onOverflowActionClicked(context, action, t);
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                });
                m.show();
            }
        }
    };

    public static class SubCont {
        final int pos;
        final Subscription sub;
        public SubCont(int pos, Subscription sub) {
            this.pos = pos;
            this.sub = sub;
        }
        public static SubCont ni(int pos, Subscription sub) {
            return new SubCont(pos, sub);
        }
    }

    public static class ViewHolder extends DragSwipeRecyclerAdapter.ViewHolder {
        @InjectView(R.id.artwork_thumb) public AnimatedImageView artwork;
        @InjectView(R.id.tile_title) public TextView title;
        @InjectView(R.id.tile_subtitle) public TextView subtitle;
        @InjectView(R.id.tile_info) public TextView extraInfo;
        @InjectView(R.id.tile_overflow) public ImageButton overflow;
        @InjectView(R.id.tile_content) public View clickableContent;
        @InjectView(R.id.drag_handle) public View dragHandle;
        @InjectView(R.id.playing_indicator) public PlayingIndicator playingIndicator;

        public final CompositeSubscription subscriptions;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
        }

        @Override
        public View getDragHandle() {
            return dragHandle;
        }

        @Override
        public View getSwipeableContainerView() {
            return clickableContent;
        }

        public void reset() {
            if (artwork != null) artwork.setImageBitmap(null);
            if (extraInfo.getVisibility() != View.GONE) {
                extraInfo.setVisibility(View.GONE);
            }
            if (playingIndicator.isAnimating()) {
                playingIndicator.stopAnimating();
            }
            if (playingIndicator.getVisibility() != View.GONE) {
                playingIndicator.setVisibility(View.GONE);
            }
            subscriptions.clear();
        }
    }
}
