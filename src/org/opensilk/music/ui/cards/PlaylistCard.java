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

package org.opensilk.music.ui.cards;

import android.content.Context;
import android.support.v7.graphics.Palette;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.ui.cards.event.CardEvent;
import org.opensilk.music.ui.cards.event.PlaylistCardClick;
import org.opensilk.music.util.MultipleArtworkLoaderTask;
import org.opensilk.music.util.PriorityAsyncTask;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/30/14.
 */
public class PlaylistCard extends AbsGenericCard<Playlist> {

    @Inject @ForFragment
    Bus mBus;

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;
    // no inject
    protected ArtworkImageView mArtwork2;
    protected ArtworkImageView mArtwork3;
    protected ArtworkImageView mArtwork4;
    protected View mDescOverlay;

    private PriorityAsyncTask mArtLoaderTask;

    public PlaylistCard(Context context, Playlist data) {
        super(context, data, determiteLayout(data));
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new PlaylistCardClick(CardEvent.OPEN, mData));
            }
        });
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        mArtwork2 = ButterKnife.findById(view, R.id.artwork_thumb2);
        mArtwork3 = ButterKnife.findById(view, R.id.artwork_thumb3);
        mArtwork4 = ButterKnife.findById(view, R.id.artwork_thumb4);
        mDescOverlay = ButterKnife.findById(view, R.id.griditem_desc_overlay);
        super.setupInnerViewElements(parent, view);
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.mPlaylistName);
        mCardSubTitle.setText(MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongNumber));
        if (mData.mAlbumNumber > 0) {
            if (isGridStyle()) {
                mArtwork.setPaletteListener(GridOverlayHelper.create(getContext(), mDescOverlay));
            }
            if (mArtwork4 != null && mArtwork3 != null && mArtwork2 != null) {
                mArtLoaderTask = new MultipleArtworkLoaderTask(getContext(), mData.mAlbumIds, mArtwork, mArtwork2, mArtwork3, mArtwork4).execute();
            } else if (mArtwork2 != null) {
                mArtLoaderTask = new MultipleArtworkLoaderTask(getContext(), mData.mAlbumIds, mArtwork, mArtwork2).execute();
            } else {
                mArtLoaderTask = new MultipleArtworkLoaderTask(getContext(), mData.mAlbumIds, mArtwork).execute();
            }
        }
    }

    @Override
    protected void cleanupViews() {
        mArtwork.setPaletteListener(null);
        super.cleanupViews();
        mArtwork2 = null;
        mArtwork3 = null;
        mArtwork4 = null;
    }

    @Override
    protected void cancelPendingTasks() {
        if (mArtLoaderTask != null) mArtLoaderTask.cancel(false);
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        if (mData.mPlaylistId != -2) {
            // cant rename or delete last added
            m.inflate(R.menu.popup_rename);
            m.inflate(R.menu.popup_delete);
        }
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    CardEvent event = CardEvent.valueOf(item.getItemId());
                    mBus.post(new PlaylistCardClick(event, mData));
                    return true;
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        throw new UnsupportedOperationException("No list for playlists");
    }

    @Override
    protected int getGridLayout() {
        return determiteLayout(mData);
    }

    private static int determiteLayout(Playlist plist) {
        if (plist.mAlbumNumber >= 4) {
            return R.layout.gridcard_artwork_quad_inner;
        } else if (plist.mAlbumNumber >= 2) {
            return R.layout.gridcard_artwork_dual_inner;
        } else {
            return R.layout.gridcard_artwork_inner;
        }
    }

}
