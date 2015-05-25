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
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.dragswipe.BaseSwipeableRecyclerAdapter;
import org.opensilk.music.ui3.common.OverflowAction;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import javax.inject.Inject;

import timber.log.Timber;

/**
 * Created by drew on 5/10/15.
 */
public class QueueScreenViewAdapter extends BaseSwipeableRecyclerAdapter<QueueScreenItem> {

    final ArtworkRequestManager requestor;
    final QueueScreenPresenter presenter;
    final PlaybackController playbackController;

    String activeId;
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
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        QueueScreenItem item = getItem(position);
        holder.reset();
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        ArtInfo artInfo = item.artInfo;
        if (artInfo == null || artInfo.equals(ArtInfo.NULLINSTANCE)) {
            setLetterTileDrawable(holder, item.title);
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
        bindClickListeners(holder, position);
        super.onBindViewHolder(holder, position);
    }

    @Override
    public long getItemId(int position) {
        QueueScreenItem item = getItem(position);
        return item.hashCode();// item.getDescription().getMediaId().hashCode() + (31 * item.getQueueId());
    }

    @Override
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

    @Override
    protected void onItemClicked(Context context, QueueScreenItem item) {
        playbackController.skipToQueueItem(item.getQueueId());
    }

    @Override
    protected void onOverflowClicked(Context context, PopupMenu menu, QueueScreenItem item) {
        menu.inflate(R.menu.popup_play_next);
        menu.inflate(R.menu.popup_add_to_playlist);
        menu.inflate(R.menu.popup_more_by_artist);
        menu.inflate(R.menu.popup_set_ringtone);
        menu.inflate(R.menu.popup_delete);
    }

    @Override
    protected boolean onOverflowActionClicked(Context context, OverflowAction action, QueueScreenItem item) {
        switch (action) {
            case PLAY_NEXT:
                playbackController.moveQueueItemToNext((int)item.getQueueId());
                return true;
            case ADD_TO_PLAYLIST:
                Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_SHORT).show();
                return true;
            case MORE_BY_ARTIST:
                Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_SHORT).show();
                return true;
            case SET_RINGTONE:
                Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_SHORT).show();
                return true;
            case DELETE:
                Toast.makeText(context, R.string.err_unimplemented, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return false;
        }
    }

}
