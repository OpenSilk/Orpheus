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
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.model.LocalAlbum;
import com.squareup.otto.Bus;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui.cards.event.AlbumCardClick;
import org.opensilk.music.ui.cards.event.AlbumCardClick.Event;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/19/14.
 */
public class AlbumCard extends AbsBundleableCard<Album> {

    @Inject @ForFragment
    Bus mBus; //Injected by adapter

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;

    public AlbumCard(Context context, Album data) {
        super(context, data, R.layout.listcard_artwork_inner);
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new AlbumCardClick(Event.OPEN, mData));
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        if (!TextUtils.isEmpty(mData.artistName)) {
            mCardSubTitle.setText(mData.artistName);
            mCardSubTitle.setVisibility(View.VISIBLE);
        } else {
            mCardSubTitle.setVisibility(View.GONE);
        }
        ArtworkManager.loadImage(new ArtInfo(mData.artistName, mData.name, mData.artworkUri), mArtwork);
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        if (mData instanceof LocalAlbum) {
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_more_by_artist);
            m.inflate(R.menu.popup_delete);
        }
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.popup_play_all:
                        mBus.post(new AlbumCardClick(Event.PLAY_ALL, mData));
                        return true;
                    case R.id.popup_shuffle_all:
                        mBus.post(new AlbumCardClick(Event.SHUFFLE_ALL, mData));
                        return true;
                    case R.id.popup_add_to_queue:
                        mBus.post(new AlbumCardClick(Event.ADD_TO_QUEUE, mData));
                        return true;
                    case R.id.popup_add_to_playlist:
                        mBus.post(new AlbumCardClick(Event.ADD_TO_PLAYLIST, mData));
                        return true;
                    case R.id.popup_more_by_artist:
                        mBus.post(new AlbumCardClick(Event.MORE_BY_ARTIST, mData));
                        return true;
                    case R.id.popup_delete:
                        mBus.post(new AlbumCardClick(Event.DELETE, mData));
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        return R.layout.listcard_artwork_inner;
    }

    @Override
    protected int getGridLayout() {
        return R.layout.gridcard_artwork_inner;
    }
}
