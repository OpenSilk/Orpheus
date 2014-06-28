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
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.model.LocalSong;
import com.squareup.otto.Bus;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui.cards.event.SongCardClick;
import org.opensilk.music.ui.cards.event.SongCardClick.Event;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/19/14.
 */
public class SongCard extends AbsCard<Song> {

    @Inject @ForFragment
    Bus mBus; //Injected by adapter

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;

    public SongCard(Context context, Song song) {
        this(context, song, R.layout.library_listcard_artwork_inner);
    }

    public SongCard(Context context, Song song, int innerLayout) {
        super(context, song, innerLayout);
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new SongCardClick(Event.PLAY, mData));
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        mCardSubTitle.setText(mData.artistName);
        ArtworkManager.loadImage(new ArtInfo(mData.albumArtistName, mData.albumName, mData.artworkUri), mArtwork);
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_next);
        m.inflate(R.menu.popup_add_to_queue);
        if (mData instanceof LocalSong) {
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_more_by_artist);
            m.inflate(R.menu.popup_set_ringtone);
            m.inflate(R.menu.popup_delete);
        }
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.popup_play_next:
                        mBus.post(new SongCardClick(Event.PLAY_NEXT, mData));
                        return true;
                    case R.id.popup_add_to_queue:
                        mBus.post(new SongCardClick(Event.ADD_TO_QUEUE, mData));
                        return true;
                    case R.id.popup_add_to_playlist:
                        mBus.post(new SongCardClick(Event.ADD_TO_PLAYLIST, mData));
                        return true;
                    case R.id.popup_more_by_artist:
                        mBus.post(new SongCardClick(Event.MORE_BY_ARTIST, mData));
                        return true;
                    case R.id.popup_set_ringtone:
                        mBus.post(new SongCardClick(Event.SET_RINGTONE, mData));
                        return true;
                    case R.id.popup_delete:
                        mBus.post(new SongCardClick(Event.DELETE, mData));
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        return R.layout.library_listcard_artwork_inner;
    }

    @Override
    protected int getGridLayout() {
        return R.layout.library_gridcard_artwork_inner;
    }

}
