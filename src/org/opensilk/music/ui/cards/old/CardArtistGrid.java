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
package org.opensilk.music.ui.cards.old;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.dialogs.AddToPlaylistDialog;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.base.BaseCard;

/**
 * Created by drew on 2/11/14.
 */
public class CardArtistGrid extends CardBaseThumb<Artist> {

    public CardArtistGrid(Context context, Artist data) {
        super(context, data);
    }

    public CardArtistGrid(Context context, Artist data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                NavUtils.openArtistProfile(getContext(), mData);
            }
        });
    }

    @Override
    protected void initHeader() {
        final CardHeaderGrid header = new CardHeaderGrid(getContext());
        header.setButtonOverflowVisible(true);
        header.setTitle(mData.mArtistName);
        header.setLineTwo(MusicUtils.makeLabel(getContext(), R.plurals.Nalbums, mData.mAlbumNumber));
        header.setPopupMenu(R.menu.card_artist, getNewHeaderPopupMenuListener());
        addCardHeader(header);
    }

    @Override
    protected void loadThumbnail(ArtworkImageView view) {
        ArtworkManager.loadArtistImage(mData.mArtistName, view);
    }

    protected CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener() {
        return new CardHeader.OnClickCardHeaderPopupMenuListener() {
            @Override
            public void onMenuItemClick(BaseCard baseCard, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.card_menu_play:
                        MusicUtils.playAll(getContext(), MusicUtils.getSongListForArtist(getContext(), mData.mArtistId), 0, false);
                        break;
                    case R.id.card_menu_shuffle:
                        MusicUtils.playAll(getContext(), MusicUtils.getSongListForArtist(getContext(), mData.mArtistId), 0, true);
                        break;
                    case R.id.card_menu_add_queue:
                        MusicUtils.addToQueue(getContext(), MusicUtils.getSongListForArtist(getContext(), mData.mArtistId));
                        break;
                    case R.id.card_menu_add_playlist:
                        AddToPlaylistDialog.newInstance(MusicUtils.getSongListForArtist(getContext(), mData.mArtistId))
                                .show(((FragmentActivity) getContext()).getSupportFragmentManager(), "AddToPlaylistDialog");
                        break;
                    case R.id.card_menu_delete:
                        final String artist = mData.mArtistName;
                        DeleteDialog.newInstance(artist, MusicUtils.getSongListForArtist(getContext(), mData.mArtistId), artist)
                                .show(((FragmentActivity) getContext()).getSupportFragmentManager(), "DeleteDialog");
                        break;
                }
            }
        };
    }
}
