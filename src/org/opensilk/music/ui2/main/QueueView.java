/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.ui2.main;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.model.RecentSong;
import com.mobeta.android.dslv.DragSortListView;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.widgets.PlayingIndicator;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import mortar.Mortar;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

/**
 * Created by drew on 10/15/14.
 */
public class QueueView extends DragSortListView implements
        DragSortListView.DropListener,
        DragSortListView.RemoveListener,
        AdapterView.OnItemClickListener {

    @Inject QueueScreen.Presenter presenter;
    @Inject ArtworkRequestManager requestor;

    Adapter adapter;

    public QueueView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Mortar.inject(getContext(), this);
        adapter = new Adapter(getContext(), requestor, presenter);
        setDropListener(this);
        setRemoveListener(this);
        setOnItemClickListener(this);
    }

    @Override
    protected void onFinishInflate() {
        Timber.v("onFinishInflate()");
        super.onFinishInflate();
        presenter.takeView(this);
    }

    @Override
    protected void onAttachedToWindow() {
        Timber.v("onAttachedToWindow()");
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        Timber.v("onDetachedFromWindow()");
        super.onDetachedFromWindow();
        presenter.dropView(this);
    }

       /*
     * RemoveListener
     */

    @Override
    public void remove(final int which) {
        RecentSong s = adapter.getItem(which);
        adapter.remove(s);
        presenter.removeQueueItem(s.recentId);
    }

    /*
     * Droplistener
     */

    @Override
    public void drop(final int from, final int to) {
        RecentSong s = adapter.getItem(from);
//        adapter.setNotifyOnChange(false);
        adapter.remove(s);
        adapter.insert(s, to);
//        adapter.setNotifyOnChange(true);
//        adapter.notifyDataSetChanged();
        presenter.moveQueueItem(from,to);
    }

    /*
     * ItemClickListener
     */

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // When selecting a track from the queue, just jump there instead of
        // reloading the queue. This is both faster, and prevents accidentally
        // dropping out of party shuffle.
        presenter.setQueuePosition(position);
    }

    public void setup() {
        setAdapter(adapter);
    }

    public void onCurrentSongChanged(long recentId) {
        adapter.currentSong = recentId;
        adapter.notifyDataSetChanged();
    }

    public void onPlaystateChanged(boolean isPlaying) {
        adapter.isPlaying = isPlaying;
        adapter.notifyDataSetChanged();
    }

    static class Adapter extends ArrayAdapter<RecentSong> {

        final ArtworkRequestManager requestor;
        final QueueScreen.Presenter presenter;

        long currentSong;
        boolean isPlaying;

        Adapter(Context context, ArtworkRequestManager requestor, QueueScreen.Presenter presenter) {
            super(context, -1);
            this.requestor = requestor;
            this.presenter = presenter;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
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
            holder.subscriptions.add(requestor.newAlbumRequest((AnimatedImageView)holder.artwork,
                    null, new ArtInfo(artist, item.albumName, item.artworkUri), ArtworkType.THUMBNAIL));

            holder.overflow.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu m = new PopupMenu(getContext(), v);
                    m.inflate(R.menu.popup_play_next);
                    if (item.isLocal) {
                        m.inflate(R.menu.popup_add_to_playlist);
                        m.inflate(R.menu.popup_more_by_artist);
                        m.inflate(R.menu.popup_set_ringtone);
                        m.inflate(R.menu.popup_delete);
                    }
                    m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem i) {
                            try {
                                return presenter.handleItemOverflowClick(OverflowAction.valueOf(i.getItemId()), item);
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
    }

    static class ViewHolder {
        final View itemView;
        @InjectView(R.id.artwork_thumb) ImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.playing_indicator) PlayingIndicator playingIndicator;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

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
