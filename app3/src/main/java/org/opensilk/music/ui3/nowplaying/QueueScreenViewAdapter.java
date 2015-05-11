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

package org.opensilk.music.ui3.nowplaying;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.playback.BundleHelper;
import org.opensilk.music.widgets.PlayingIndicator;

import android.net.Uri;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.gson.Gson;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 5/10/15.
 */
public class QueueScreenViewAdapter extends RecyclerListAdapter<QueueItem, QueueScreenViewAdapter.ViewHolder> {

    final ArtworkRequestManager requestor;

    String activeId;
    boolean isPlaying;

    @Inject
    public QueueScreenViewAdapter(ArtworkRequestManager requestor) {
        this.requestor = requestor;
        setHasStableIds(true);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, R.layout.gallery_list_item_dragsort));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.reset();
        QueueItem item = getItem(position);
        holder.title.setText(item.getDescription().getTitle());
        holder.subtitle.setText(item.getDescription().getSubtitle());
        holder.subscriptions.add(
                requestor.newRequest(holder.artwork, null,
                        BundleHelper.<ArtInfo>getParcelable(item.getDescription().getExtras()),
                        ArtworkType.THUMBNAIL
                )
        );
        if (StringUtils.equals(activeId, item.getDescription().getMediaId())) {
            if (isPlaying) {
                holder.playingIndicator.startAnimating();
            } else {
                holder.playingIndicator.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getDescription().getMediaId().hashCode();
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.reset();
    }

    public void setActiveItem(String id) {
        if (!StringUtils.equals(activeId, id)) {
            activeId = id;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public void setPlaying(boolean playing) {
        if (isPlaying != playing) {
            isPlaying = playing;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @InjectView(R.id.artwork_thumb) AnimatedImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.playing_indicator) PlayingIndicator playingIndicator;
        @InjectView(R.id.tile_overflow) ImageButton overflow;
        @InjectView(R.id.tile_content) View clickableContent;

        final CompositeSubscription subscriptions;

        public ViewHolder(View itemView) {
            super(itemView);
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
