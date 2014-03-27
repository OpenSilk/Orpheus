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
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.dialogs.AddToPlaylistDialog;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 2/11/14.
 */
public class CardAlbumList extends CardBaseListNoHeader<Album> {

    public CardAlbumList(Context context, Album data) {
        super(context, data);
    }

    public CardAlbumList(Context context, Album data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        mTitle = mData.mAlbumName;
        mSubTitle = mData.mArtistName;
        mSubTitleAlt = mData.mYear;
        if (mData.mSongNumber > 0) {
            mExtraText = MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongNumber);
        }

        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                NavUtils.openAlbumProfile(getContext(), mData);
            }
        });
    }

    protected void loadThumbnail(ArtworkImageView view) {
        ArtworkManager.loadAlbumImage(mData.mArtistName, mData.mAlbumName, mData.mAlbumId, view);
    }

    @Override
    public int getOverflowMenuId() {
        return R.menu.card_album;
    }

    public PopupMenu.OnMenuItemClickListener getOverflowPopupMenuListener() {
        return new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
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
                        NavUtils.openArtistProfile(getContext(), MusicUtils.makeArtist(getContext(), mData.mArtistName));
                        break;
                    case R.id.card_menu_delete:
                        final String album = mData.mAlbumName;
                        DeleteDialog.newInstance(album, MusicUtils.getSongListForAlbum(getContext(), mData.mAlbumId),
                                /*ImageFetcher.generateAlbumCacheKey(album, mData.mArtistName)*/ null) //TODO
                                .show(((FragmentActivity) getContext()).getSupportFragmentManager(), "DeleteDialog");
                        break;
                }
                return false;
            }
        };
    }

}
