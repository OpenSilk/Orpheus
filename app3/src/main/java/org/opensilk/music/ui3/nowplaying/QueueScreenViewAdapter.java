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
import org.opensilk.common.core.rx.SimpleObserver;
import org.opensilk.common.ui.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.playback.BundleHelper;
import org.opensilk.music.ui3.dragswipe.BaseDragSwipeRecyclerAdapter;
import org.opensilk.music.ui3.dragswipe.DragSwipeRecyclerAdapter;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.widgets.PlayingIndicator;

import android.content.Context;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;
import rx.Observer;
import rx.Subscription;
import rx.android.events.OnClickEvent;
import rx.android.observables.ViewObservable;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 5/10/15.
 */
public class QueueScreenViewAdapter extends BaseDragSwipeRecyclerAdapter<QueueItem> {

    final ArtworkRequestManager requestor;
    final QueueScreenPresenter presenter;

    String activeId;
    boolean isPlaying;

    @Inject
    public QueueScreenViewAdapter(
            ArtworkRequestManager requestor,
            QueueScreenPresenter presenter
    ) {
        super();
        this.requestor = requestor;
        this.presenter = presenter;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        QueueItem item = getItem(position);
        holder.title.setText(item.getDescription().getTitle());
        holder.subtitle.setText(item.getDescription().getSubtitle());
        ArtInfo artInfo = BundleHelper.getParcelable(item.getDescription().getExtras());
        if (artInfo == null || artInfo.equals(ArtInfo.NULLINSTANCE)) {
            setLetterTileDrawable(holder, item.getDescription().getTitle().toString());
        } else {
            holder.subscriptions.add(
                    requestor.newRequest(holder.artwork, null, artInfo, ArtworkType.THUMBNAIL));
        }
        if (StringUtils.equals(activeId, item.getDescription().getMediaId())) {
            if (isPlaying) {
                holder.playingIndicator.startAnimating();
            } else {
                holder.playingIndicator.setVisibility(View.VISIBLE);
            }
        }
        bindClickListeners(holder, position);
        super.onBindViewHolder(holder, position);
    }

    @Override
    public long getItemId(int position) {
        return new Random(position).nextLong();
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

    @Override
    protected void onItemClicked(Context context, QueueItem item) {

    }

    @Override
    protected void onOverflowClicked(Context context, PopupMenu menu, QueueItem item) {

    }

    @Override
    protected boolean onOverflowActionClicked(Context context, OverflowAction action, QueueItem item) {
        return false;
    }

    @Override
    protected void onItemRemoved(Context context, int position, QueueItem item) {

    }
}
