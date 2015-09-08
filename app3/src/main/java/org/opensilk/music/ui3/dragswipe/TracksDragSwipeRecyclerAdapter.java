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
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.ui3.common.BundleablePresenter;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.UtilsCommon;

import java.util.Map;
import java.util.WeakHashMap;

import javax.inject.Inject;

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
public class TracksDragSwipeRecyclerAdapter extends BaseDragSwipeRecyclerAdapter<Bundleable> {

    final TracksDragSwipePresenter presenter;

    @Inject
    public TracksDragSwipeRecyclerAdapter(TracksDragSwipePresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Track track = (Track) getItem(position);
        ArtInfo artInfo = UtilsCommon.makeBestfitArtInfo(track.albumArtistName, track.artistName, track.albumName, track.artworkUri);
        holder.title.setText(track.getDisplayName());
        holder.subtitle.setText(track.artistName);
        if (holder.extraInfo != null && track.duration > 0) {
            holder.extraInfo.setText(UtilsCommon.makeTimeString(holder.itemView.getContext(), track.duration));
            holder.extraInfo.setVisibility(View.VISIBLE);
        }
        if (artInfo == ArtInfo.NULLINSTANCE) {
            setLetterTileDrawable(holder, track.getDisplayName());
        } else {
            holder.subscriptions.add(presenter.getRequestor().newRequest(holder.artwork,
                    null, artInfo, ArtworkType.THUMBNAIL));
        }
        bindClickListeners(holder, position);
        super.onBindViewHolder(holder, position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    protected void onItemClicked(Context context, Bundleable item) {
        presenter.onItemClicked(context, item);
    }

    @Override
    protected void onOverflowClicked(Context context, PopupMenu menu, Bundleable item) {
        presenter.onOverflowClicked(context, menu, item);
    }

    @Override
    protected boolean onOverflowActionClicked(Context context, OverflowAction action, Bundleable item) {
        return presenter.onOverflowActionClicked(context, action, item);
    }

    @Override
    protected void onItemRemoved(Context context, int position, Bundleable item) {
        presenter.startActionMode();
    }

    @Override
    public void onMoveItem(int fromPosition, int toPosition) {
        super.onMoveItem(fromPosition, toPosition);
        if (fromPosition != toPosition) {
            presenter.startActionMode();
        }
    }
}
