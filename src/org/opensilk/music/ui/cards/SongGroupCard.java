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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.ui.cards.event.CardEvent;
import org.opensilk.music.ui.cards.event.SongGroupCardClick;
import org.opensilk.music.util.MultipleArtworkLoaderTask;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 7/10/14.
 */
public class SongGroupCard extends AbsGenericCard<LocalSongGroup> {

    @Inject @ForFragment
    Bus mBus;

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;
    // no inject
    protected ArtworkImageView mArtwork2;
    protected ArtworkImageView mArtwork3;
    protected ArtworkImageView mArtwork4;

    public SongGroupCard(Context context, LocalSongGroup data) {
        super(context, data, determiteLayout(data));
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new SongGroupCardClick(CardEvent.OPEN, mData));
            }
        });
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        mArtwork2 = ButterKnife.findById(view, R.id.artwork_thumb2);
        mArtwork3 = ButterKnife.findById(view, R.id.artwork_thumb3);
        mArtwork4 = ButterKnife.findById(view, R.id.artwork_thumb4);
        super.setupInnerViewElements(parent, view);
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        String l2 = MusicUtils.makeLabel(getContext(), R.plurals.Nalbums, mData.albumIds.length)
                + ", " + MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.songIds.length);
        mCardSubTitle.setText(l2);
        if (mData.albumIds.length > 0){
            if (mArtwork4 != null && mArtwork3 != null && mArtwork2 != null) {
                new MultipleArtworkLoaderTask(getContext(), mData.albumIds, mArtwork, mArtwork2, mArtwork3, mArtwork4).execute();
            } else if (mArtwork2 != null) {
                new MultipleArtworkLoaderTask(getContext(), mData.albumIds, mArtwork, mArtwork2).execute();
            } else {
                new MultipleArtworkLoaderTask(getContext(), mData.albumIds, mArtwork).execute();
            }
        }
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        m.inflate(R.menu.popup_add_to_playlist);
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                try {
                    CardEvent event = CardEvent.valueOf(item.getItemId());
                    mBus.post(new SongGroupCardClick(event, mData));
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
        throw new UnsupportedOperationException("No list for song groups");
    }

    @Override
    protected int getGridLayout() {
        return determiteLayout(mData);
    }

    private static int determiteLayout(LocalSongGroup data) {
        if (data.albumIds.length >= 4) {
            return R.layout.gridcard_artwork_quad_inner;
        } else if (data.albumIds.length >= 2) {
            return R.layout.gridcard_artwork_dual_inner;
        } else {
            return R.layout.gridcard_artwork_inner;
        }
    }

}
