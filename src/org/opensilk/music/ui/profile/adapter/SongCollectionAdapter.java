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

package org.opensilk.music.ui.profile.adapter;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.util.CursorHelpers;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import mortar.Mortar;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 7/10/14.
 */
public class SongCollectionAdapter extends ArrayAdapter<LocalSong> {

    protected final boolean useSimpleLayout;

    @Inject OverflowHandlers.LocalSongs overflowHandler;
    @Inject ArtworkRequestManager requestor;

    public SongCollectionAdapter(Context context,
                                 boolean useSimpleLayout) {
        super(context, 0);
        this.useSimpleLayout = useSimpleLayout;
        Mortar.inject(context, this);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = newView(parent.getContext(), parent);
        }
        bindView(v, getItem(position));
        return v;
    }

    public View newView(Context context, ViewGroup parent) {
        View v;
        if (useSimpleLayout) {
            v = LayoutInflater.from(context).inflate(R.layout.gallery_list_item_simple, parent, false);
        } else {
            v = LayoutInflater.from(context).inflate(R.layout.gallery_list_item_artwork, parent, false);
        }
        v.setTag(new ViewHolder(v));
        return v;
    }

    public void bindView(View view, final LocalSong song) {
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.reset();
        holder.title.setText(song.name);
        holder.subtitle.setText(song.artistName);
        holder.info.setText(MusicUtils.makeTimeString(view.getContext(), song.duration));
        if (!useSimpleLayout && holder.artwork != null) {
            holder.subscriptions.add(requestor.newAlbumRequest(holder.artwork,
                    null, song.albumId, ArtworkType.THUMBNAIL));
        }
        holder.overflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu m = new PopupMenu(v.getContext(), v);
                overflowHandler.populateMenu(m, song);
                onPostPopulateOverflow(m);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            return overflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), song);
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                });
                m.show();
            }
        });
        holder.clicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = 0; int count = getCount();
                LocalSong[] songs = new LocalSong[count];
                for (int ii=0; ii<count; ii++) {
                    songs[ii] = getItem(ii);
                    if (getItem(ii).songId == song.songId) {
                        pos = ii;
                    }
                }
                overflowHandler.playAll(songs, pos);
            }
        });
    }

    protected void onPostPopulateOverflow(PopupMenu m) {

    }

    public static class ViewHolder {
        final View itemView;
        @InjectView(R.id.artwork_thumb) @Optional AnimatedImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_info) TextView info;
        @InjectView(R.id.tile_overflow) ImageButton overflow;
        @InjectView(R.id.tile_content) View clicker;

        final CompositeSubscription subscriptions;

        public ViewHolder(View itemView) {
            this.itemView = itemView;
            ButterKnife.inject(this, itemView);
            info.setVisibility(View.VISIBLE);
            subscriptions = new CompositeSubscription();
        }

        public void reset() {
//            Timber.v("Reset title=%s", title.getText());
            if (artwork != null) artwork.setImageBitmap(null);
            subscriptions.clear();
        }

    }

}
