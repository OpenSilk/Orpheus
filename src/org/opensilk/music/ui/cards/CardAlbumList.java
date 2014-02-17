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
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.dialogs.AddToPlaylistDialog;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.base.BaseCard;

/**
 * Created by drew on 2/11/14.
 */
public class CardAlbumList extends CardBaseList<Album> {

    public CardAlbumList(Context context, Album data) {
        this(context, data, R.layout.card_list_item_inner);
    }

    public CardAlbumList(Context context, Album data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        mTitle = mData.mAlbumName;
        mSecondTitle = mData.mArtistName;
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                NavUtils.openAlbumProfile(getContext(), mData.mAlbumName, mData.mArtistName, mData.mAlbumId);
            }
        });
    }

    protected void loadThumbnail(ImageFetcher fetcher, ImageView view) {
        fetcher.loadAlbumImage(mData.mArtistName, mData.mAlbumName, mData.mAlbumId, view);
    }

    @Override
    protected int getHeaderMenuId() {
        return R.menu.card_album;
    }

    protected CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener() {
        return new CardHeader.OnClickCardHeaderPopupMenuListener() {
            @Override
            public void onMenuItemClick(BaseCard baseCard, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.card_menu_play:
                        MusicUtils.playAll(getContext(), MusicUtils.getSongListForAlbum(getContext(), mData.mAlbumId), 0, false);
                        break;
                    case R.id.card_menu_add_queue:
                        MusicUtils.addToQueue(getContext(), MusicUtils.getSongListForAlbum(getContext(), mData.mAlbumId));
                        break;
                    case R.id.card_menu_add_playlist:
                        AddToPlaylistDialog.newInstance(MusicUtils.getSongListForAlbum(getContext(), mData.mAlbumId))
                                .show(((FragmentActivity) getContext()).getSupportFragmentManager(), "AddToPlaylistDialog");
                        break;
                    case R.id.card_menu_more_by:
                        NavUtils.openArtistProfile(getContext(), mData.mArtistName);
                        break;
                    case R.id.card_menu_delete:
                        final String album = mData.mAlbumName;
                        DeleteDialog.newInstance(album, MusicUtils.getSongListForAlbum(getContext(), mData.mAlbumId),
                                ImageFetcher.generateAlbumCacheKey(album, mData.mArtistName))
                                .show(((FragmentActivity) getContext()).getSupportFragmentManager(), "DeleteDialog");
                        break;
                }
            }
        };
    }

}
