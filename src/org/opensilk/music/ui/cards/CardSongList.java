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
import android.widget.ImageView;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.dialogs.AddToPlaylistDialog;

/**
 * Created by drew on 2/11/14.
 */
public class CardSongList extends CardBaseList<Song> {

    public CardSongList(Context context, Song data) {
        this(context, data, R.layout.card_list_thumb_inner_layout);
    }

    public CardSongList(Context context, Song data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        mTitle = mData.mSongName;
        mSubTitle = mData.mArtistName;
        mExtraText = MusicUtils.makeTimeString(getContext(),mData.mDuration);
        //TODO onclick
    }

    @Override
    protected void loadThumbnail(ImageFetcher fetcher, ImageView view) {
        fetcher.loadAlbumImage(mData.mArtistName, mData.mAlbumName, mData.mAlbumId, view);
    }

    @Override
    public int getOverflowMenuId() {
        return R.menu.card_song;
    }

    @Override
    public PopupMenu.OnMenuItemClickListener getOverflowPopupMenuListener() {
        return new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.card_menu_play:
                        MusicUtils.playAll(getContext(), new long[]{
                                mData.mSongId
                        }, 0, false);
                        break;
                    case R.id.card_menu_play_next:
                        MusicUtils.playNext(new long[] {
                                mData.mSongId
                        });
                        break;
                    case R.id.card_menu_add_queue:
                        MusicUtils.addToQueue(getContext(), new long[] {
                                mData.mSongId
                        });
                        break;
                    case R.id.card_menu_add_playlist:
                        AddToPlaylistDialog.newInstance(new long[]{
                                mData.mSongId
                        }).show(((FragmentActivity) getContext()).getSupportFragmentManager(), "AddToPlaylistDialog");
                        break;
                    case R.id.card_menu_more_by:
                        NavUtils.openArtistProfile(getContext(), mData.mArtistName);
                        break;
                    case R.id.card_menu_set_ringtone:
                        MusicUtils.setRingtone(getContext(), mData.mSongId);
                        break;
                    case R.id.card_menu_delete:
                        final String song = mData.mSongName;
                        DeleteDialog.newInstance(song, new long[]{
                                mData.mSongId
                        }, null).show(((FragmentActivity) getContext()).getSupportFragmentManager(), "DeleteDialog");
                        break;
                }
                return false;
            }
        };
    }
}
