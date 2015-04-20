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

package org.opensilk.music.ui2.queue;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.model.RecentSong;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.widgets.PlayingIndicator;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 4/20/15.
 */
class QueueScreenAdapter extends ArrayAdapter<RecentSong> {

    final QueueScreenPresenter presenter;

    long currentSong;
    boolean isPlaying;

    QueueScreenAdapter(Context context, QueueScreenPresenter presenter) {
        super(context, -1);
        this.presenter = presenter;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder holder;
        if (v == null) {
            v = LayoutInflater.from(getContext()).inflate(R.layout.gallery_list_item_dragsort, parent, false);
            holder = new ViewHolder(v);
            v.setTag(holder);
        } else {
            holder = (ViewHolder) v.getTag();
            holder.reset();
        }
        final RecentSong item = getItem(position);
        holder.title.setText(item.name);
        holder.subtitle.setText(item.artistName);

        String artist = item.albumArtistName;
        if (TextUtils.isEmpty(artist)) artist = item.artistName;
        holder.subscriptions.add(presenter.requestor.newAlbumRequest(holder.artwork,
                null, new ArtInfo(artist, item.albumName, item.artworkUri), ArtworkType.THUMBNAIL));

        holder.clickableContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.setQueuePosition(position);
            }
        });

        holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu m = new PopupMenu(getContext(), v);
                presenter.overflowHandler.populateMenu(m, item);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem i) {
                        try {
                            return presenter.overflowHandler
                                    .handleClick(OverflowAction.valueOf(i.getItemId()), item);
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                });
                m.show();
            }
        });

        if (currentSong == item.recentId) {
            if (isPlaying) {
                holder.playingIndicator.startAnimating();
            } else {
                holder.playingIndicator.setVisibility(View.VISIBLE);
            }
        }

        return v;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).identity.hashCode();
    }

    static class ViewHolder {
        final View itemView;
        @InjectView(R.id.artwork_thumb) AnimatedImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.playing_indicator) PlayingIndicator playingIndicator;
        @InjectView(R.id.tile_overflow) ImageButton overflow;
        @InjectView(R.id.tile_content) View clickableContent;

        final CompositeSubscription subscriptions;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
            ButterKnife.inject(this, itemView);
            subscriptions = new CompositeSubscription();
        }

        public void reset() {
//            Timber.v("Reset title=%s", title.getText());
            if (artwork != null) artwork.setImageBitmap(null);
            subscriptions.clear();
            if (playingIndicator.isAnimating()) {
                playingIndicator.stopAnimating(); //stopAnimating sets GONE
            } else {
                playingIndicator.setVisibility(View.GONE);
            }
        }

    }
}
