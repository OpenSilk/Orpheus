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
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.dialogs.AddToPlaylistDialog;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.base.BaseCard;

/**
 * Created by drew on 2/11/14.
 */
public class CardAlbumGrid extends CardBaseThumb<Album> {

    public CardAlbumGrid(Context context, Album data) {
        super(context, data);
    }

    public CardAlbumGrid(Context context, Album data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                NavUtils.openAlbumProfile(getContext(), mData);
            }
        });
    }

    @Override
    protected void initHeader() {
        CardHeaderGrid header = new CardHeaderGrid(getContext());
        header.setButtonOverflowVisible(true);
        header.setTitle(mData.name);
        header.setLineTwo(mData.artistName);
        header.setPopupMenu(R.menu.card_album, getNewHeaderPopupMenuListener());
        addCardHeader(header);
    }

    protected void loadThumbnail(ArtworkImageView view) {
        ArtworkManager.loadAlbumImage(mData.artistName, mData.name, mData.artworkUri, view);
    }

    protected CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener() {
        return new CardHeader.OnClickCardHeaderPopupMenuListener() {
            @Override
            public void onMenuItemClick(BaseCard baseCard, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.card_menu_play:
                        MusicUtils.playAll(getContext(), MusicUtils.getSongListForAlbum(getContext(), Long.decode(mData.identity)), 0, false);
                        break;
                    case R.id.card_menu_shuffle:
                        MusicUtils.playAll(getContext(), MusicUtils.getSongListForAlbum(getContext(), Long.decode(mData.identity)), 0, true);
                        break;
                    case R.id.card_menu_add_queue:
                        MusicUtils.addToQueue(getContext(), MusicUtils.getSongListForAlbum(getContext(), Long.decode(mData.identity)));
                        break;
                    case R.id.card_menu_add_playlist:
                        AddToPlaylistDialog.newInstance(MusicUtils.getSongListForAlbum(getContext(), Long.decode(mData.identity)))
                                .show(((FragmentActivity) getContext()).getSupportFragmentManager(), "AddToPlaylistDialog");
                        break;
                    case R.id.card_menu_more_by:
                        NavUtils.openArtistProfile(getContext(), MusicUtils.makeArtist(getContext(), mData.artistName));
                        break;
                    case R.id.card_menu_delete:
                        final String album = mData.name;
                        DeleteDialog.newInstance(album, MusicUtils.getSongListForAlbum(getContext(), Long.decode(mData.identity)),
                                /*ImageFetcher.generateAlbumCacheKey(album, mData.mArtistName)*/ null) //TODO
                                .show(((FragmentActivity) getContext()).getSupportFragmentManager(), "DeleteDialog");
                        break;
                }
            }
        };
    }

}
