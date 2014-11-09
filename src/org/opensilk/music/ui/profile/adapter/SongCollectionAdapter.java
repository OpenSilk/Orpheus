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
import android.widget.CursorAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.cards.SongCollectionCard;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.widgets.GridTileDescription;
import org.opensilk.silkdagger.DaggerInjector;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardCursorAdapter;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by drew on 7/10/14.
 */
public class SongCollectionAdapter extends CursorAdapter {

    protected final boolean useSimpleLayout;
    protected final Uri uri;
    protected final String[] projection;
    protected final String selection;
    protected final String[] selectionArgs;
    protected final String sortOrder;

    final OverflowHandlers.LocalSongs overflowHandler;
    final ArtworkRequestManager requestor;

    public SongCollectionAdapter(Context context,
                                 OverflowHandlers.LocalSongs overflowHandler,
                                 ArtworkRequestManager requestor,
                                 boolean useSimpleLayout,
                                 Uri uri, String[] projection, String selection,
                                 String[] selectionArgs, String sortOrder) {
        super(context, null, 0);
        this.overflowHandler = overflowHandler;
        this.requestor = requestor;
        this.useSimpleLayout = useSimpleLayout;
        this.uri = uri;
        this.projection = projection;
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.sortOrder = sortOrder;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v;
        if (useSimpleLayout) {
            v = LayoutInflater.from(context).inflate(R.layout.gallery_list_item_simple, parent, false);
        } else {
            v = LayoutInflater.from(context).inflate(R.layout.gallery_list_item_artwork, parent, false);
        }
        v.setTag(new ViewHolder(v));
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        final LocalSong song = CursorHelpers.makeLocalSongFromCursor(cursor);
        ViewHolder holder = (ViewHolder) view.getTag();
        holder.reset();
        holder.title.setText(song.name);
        holder.subtitle.setText(song.artistName);
        holder.info.setText(MusicUtils.makeTimeString(context, song.duration));
        if (!useSimpleLayout && holder.artwork != null) {
            holder.subscriptions.add(requestor.newAlbumRequest((AnimatedImageView) holder.artwork,
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
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                overflowHandler.play(song);
            }
        });
    }

    protected void onPostPopulateOverflow(PopupMenu m) {

    }

    public static class ViewHolder {
        final View itemView;
        @InjectView(R.id.artwork_thumb) @Optional ImageView artwork;
        @InjectView(R.id.tile_title) TextView title;
        @InjectView(R.id.tile_subtitle) TextView subtitle;
        @InjectView(R.id.tile_info) TextView info;
        @InjectView(R.id.tile_overflow) ImageButton overflow;

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
