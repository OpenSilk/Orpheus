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
import org.opensilk.common.ui.recycler.DragSwipeViewHolder;
import org.opensilk.common.ui.recycler.RecyclerListAdapter;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.common.ui.widget.LetterTileDrawable;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui.widget.PlayingIndicator;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 5/10/15.
 */
public class QueueScreenViewAdapter extends RecyclerListAdapter<QueueItem, QueueScreenViewAdapter.ViewHolder> {

    final ArtworkRequestManager requestor;
    final QueueScreenPresenter presenter;
    final PlaybackController playbackController;

    long activeId = -1;
    boolean isPlaying;

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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(inflate(parent, R.layout.gallery_list_item_dragsort));
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
                holder.subscriptions.add(
                        requestor.newRequest(holder.artwork, null, artInfo, ArtworkType.THUMBNAIL)
                );
            } else {
                setLetterTileDrawable(holder, desc.getTitle().toString());
            }
        }
        if (activeId == item.getQueueId()) {
            if (isPlaying) {
                holder.playingIndicator.startAnimating();
            } else {
                holder.playingIndicator.setVisibility(View.VISIBLE);
            }
        }
    }

    void setLetterTileDrawable(ViewHolder holder, String text) {
        Resources resources = holder.itemView.getResources();
        LetterTileDrawable drawable = LetterTileDrawable.fromText(resources, text);
        drawable.setIsCircular(true);
        holder.artwork.setImageDrawable(drawable);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getQueueId();
    }

    public void setActiveItem(long newId) {
        if (activeId != newId) {
            int oldidx = -1;
            int activeidx = -1;
            for (int ii=0; ii<getItemCount(); ii++) {
                long id = getItemId(ii);
                if (activeId == id) {
                    oldidx = ii;
                } else if (newId == id) {
                    activeidx = ii;
                }
                if (activeidx != -1 && oldidx != -1) {
                    break;
                }
            }
            activeId = newId;
            notifyActive(oldidx);
            notifyActive(activeidx);
            Timber.v("Active item updated %d", newId);
        }
    }

    private void notifyActive(int idx) {
        if (idx >= 0) {
            notifyItemChanged(idx);
        }
    }

    public void setPlaying(boolean playing) {
        if (isPlaying != playing) {
            isPlaying = playing;
            int activeidx = -1;
            for (int ii=0; ii<getItemCount(); ii++) {
                long id = getItemId(ii);
                if (activeId == id) {
                    activeidx = ii;
                    break;
                }
            }
            notifyActive(activeidx);
            Timber.v("Playing updated playing=%s", playing);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements DragSwipeViewHolder {
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.artwork_thumb) AnimatedImageView artwork;
        @InjectView(R.id.drag_handle) View dragHandle;
        @InjectView(R.id.playing_indicator) PlayingIndicator playingIndicator;
        final CompositeSubscription subscriptions = new CompositeSubscription();
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.inject(this, itemView);
        }
        void reset() {
            subscriptions.clear();
            artwork.setImageBitmap(null);
            playingIndicator.stopAnimating();
            playingIndicator.setVisibility(View.GONE);
        }

        @Override
        public View getDragHandle() {
            return dragHandle;
        }

        @Override
        public void onItemSelected() {
            itemView.setBackgroundColor(Color.BLUE);
        }

        @Override
        public void onItemClear() {
            itemView.setBackground(null);
        }
    }

}
