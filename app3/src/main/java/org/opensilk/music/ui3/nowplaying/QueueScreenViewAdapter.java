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
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui.widget.PlayingIndicator;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 5/10/15.
 */
public class QueueScreenViewAdapter extends RecyclerListAdapter<QueueScreenItem, QueueScreenViewAdapter.ViewHolder> {

    final ArtworkRequestManager requestor;
    final QueueScreenPresenter presenter;
    final PlaybackController playbackController;

    String activeId;
    boolean isPlaying;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView subtitle;
        AnimatedImageView artwork;
        PlayingIndicator playingIndicator;

        final CompositeSubscription subscriptions = new CompositeSubscription();
        public ViewHolder(View itemView) {
            super(itemView);
        }
        void reset() {
            subscriptions.clear();
        }
    }

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
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        QueueScreenItem item = getItem(position);
        holder.reset();
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        ArtInfo artInfo = item.artInfo;
        if (artInfo == null || artInfo.equals(ArtInfo.NULLINSTANCE)) {
//            setLetterTileDrawable(holder, item.title);
        } else {
            holder.subscriptions.add(
                    requestor.newRequest(holder.artwork, null, artInfo, ArtworkType.THUMBNAIL));
        }
        if (StringUtils.equals(activeId, item.mediaId)) {
            if (isPlaying) {
                holder.playingIndicator.startAnimating();
            } else {
                holder.playingIndicator.setVisibility(View.VISIBLE);
            }
        }
//        holder.title.setText(item.getDescription().getTitle());
//        holder.subtitle.setText(item.getDescription().getSubtitle());
//        ArtInfo artInfo = BundleHelper.getParcelable(item.getDescription().getExtras());
//        if (artInfo == null || artInfo.equals(ArtInfo.NULLINSTANCE)) {
//            setLetterTileDrawable(holder, item.getDescription().getTitle().toString());
//        } else {
//            holder.subscriptions.add(
//                    requestor.newRequest(holder.artwork, null, artInfo, ArtworkType.THUMBNAIL));
//        }
//        if (StringUtils.equals(activeId, item.getDescription().getMediaId())) {
//            if (isPlaying) {
//                holder.playingIndicator.startAnimating();
//            } else {
//                holder.playingIndicator.setVisibility(View.VISIBLE);
//            }
//        }
//        bindClickListeners(holder, position);
    }

    @Override
    public long getItemId(int position) {
        QueueScreenItem item = getItem(position);
        return item.hashCode();// item.getDescription().getMediaId().hashCode() + (31 * item.getQueueId());
    }

//    @Override
    protected void onItemRemoved(Context context, int position, QueueScreenItem item) {
        playbackController.removeQueueItemAt(position);
    }

    public void setActiveItem(String id) {
        if (!StringUtils.equals(activeId, id)) {
            activeId = id;
            Timber.v("Active item updated %s", id);
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public void setPlaying(boolean playing) {
        if (isPlaying != playing) {
            isPlaying = playing;
            Timber.v("Playing updated playing=%s", playing);
            notifyItemRangeChanged(0, getItemCount());
        }
    }

//    @Override
    protected void onItemClicked(Context context, QueueScreenItem item) {
        playbackController.skipToQueueItem(item.getQueueId());
    }

}
