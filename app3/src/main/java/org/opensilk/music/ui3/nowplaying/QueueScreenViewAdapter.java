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

import android.content.res.Resources;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.ui.recycler.DragSwipeViewHolder;
import org.opensilk.common.ui.recycler.ItemClickSupport;
import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.common.ui.util.ThemeUtils;
import org.opensilk.common.ui.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui.widget.PlayingIndicator;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

/**
 * Created by drew on 5/10/15.
 */
public class QueueScreenViewAdapter extends RecyclerListAdapter<QueueItem,
        QueueScreenViewAdapter.ViewHolder> implements ItemClickSupport.OnItemClickListener {

    final ArtworkRequestManager requestor;
    final QueueScreenPresenter presenter;
    final PlaybackController playbackController;

    private final Object INDICATOR_UPDATE = new Object();

    @Inject
    public QueueScreenViewAdapter(
            ArtworkRequestManager requestor,
            QueueScreenPresenter presenter,
            PlaybackController playbackController
    ) {
        super();
        this.requestor = requestor;
        this.presenter = presenter;
        this.playbackController = playbackController;
        setHasStableIds(true);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        ItemClickSupport.addTo(recyclerView)
                .setOnItemClickListener(this);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        ItemClickSupport.removeFrom(recyclerView);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, viewType));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        QueueItem item = getItem(position);
        holder.reset();
        MediaDescriptionCompat desc = item.getDescription();
        holder.title.setText(desc.getTitle());
        holder.subtitle.setText(desc.getSubtitle());
        Uri uri = desc.getIconUri();
        if (uri != null) {
            ArtInfo artInfo = ArtInfo.fromUri(uri);
            if (artInfo !=ArtInfo.NULLINSTANCE) {
                requestor.newRequest(artInfo, holder.artwork, BundleHelper.b().putInt(1).get());
            } else {
                CharSequence titlee = desc.getTitle();
                setLetterTileDrawable(holder, titlee != null ? titlee.toString() : "");
            }
        }
        setItemActive(holder, item);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        if (isIndicatorUpdate(payloads)) {
            setItemActive(holder, getItem(position));
        } else {
            onBindViewHolder(holder, position);
        }
    }

    void setItemActive(ViewHolder holder, QueueItem item) {
        if (presenter.lastPlayingId == item.getQueueId()) {
            if (presenter.isPlaying) {
                holder.playingIndicator.startAnimating();
            } else {
                holder.playingIndicator.setVisibility(View.VISIBLE);
            }
        } else {
            holder.playingIndicator.stopAnimating();
            holder.playingIndicator.setVisibility(View.GONE);
        }
    }

    void setLetterTileDrawable(ViewHolder holder, String text) {
        Resources resources = holder.itemView.getResources();
        LetterTileDrawable drawable = LetterTileDrawable.fromText(resources, text);
        drawable.setIsCircular(true);
        holder.artwork.setImageDrawable(drawable);
    }

    boolean isIndicatorUpdate(List<Object> payloads) {
        if(payloads == null || payloads.isEmpty()) {
            return false;
        }
        for (Object pl: payloads) {
            if (pl != INDICATOR_UPDATE) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.gallery_list_item_artwork;
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getQueueId();
    }

    public void poke() {
        notifyItemRangeChanged(0, getItemCount(), INDICATOR_UPDATE);
    }

    @Override
    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
        presenter.onItemClicked(getItem(position));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements DragSwipeViewHolder {
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.artwork_thumb) ImageView artwork;
        @InjectView(R.id.playing_indicator) PlayingIndicator playingIndicator;
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
        void reset() {
            artwork.setImageBitmap(null);
            stopAnimating();
        }

        void stopAnimating() {
            playingIndicator.stopAnimating();
            playingIndicator.setVisibility(View.GONE);
        }

        @Override
        public View getDragHandle() {
            return artwork;
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(ThemeUtils.getColorAccent(itemView.getContext()));
        }

        @Override
        public void onItemClear() {
            //noinspection deprecation
            itemView.setBackgroundDrawable(null);
        }

    }

}
