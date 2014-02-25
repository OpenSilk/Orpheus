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
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.Song;
import com.andrew.apollo.ui.activities.ProfileActivity;
import com.andrew.apollo.utils.MusicUtils;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.base.BaseCard;

/**
 * Created by drew on 2/16/14.
 */
public class CardGenreGrid extends CardBaseThumb<Genre> {

    public CardGenreGrid(Context context, Genre data) {
        super(context, data);
    }

    public CardGenreGrid(Context context, Genre data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                // Create a new bundle to transfer the artist info
                final Bundle bundle = new Bundle();
                bundle.putLong(Config.ID, mData.mGenreId);
                bundle.putString(Config.MIME_TYPE, MediaStore.Audio.Genres.CONTENT_TYPE);
                bundle.putString(Config.NAME, mData.mGenreName);

                // Create the intent to launch the profile activity
                final Intent intent = new Intent(getContext(), ProfileActivity.class);
                intent.putExtras(bundle);
                getContext().startActivity(intent);
            }
        });
    }

    @Override
    protected void initHeader() {
        final CardHeaderGrid header = new CardHeaderGrid(getContext());
        header.setButtonOverflowVisible(true);
        header.setTitle(mData.mGenreName);
        header.setLineTwo(MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongs.size()));
        header.setPopupMenu(R.menu.card_genre, getNewHeaderPopupMenuListener());
        addCardHeader(header);
    }

    @Override
    protected void loadThumbnail(ImageFetcher fetcher, ImageView view) {
        if (mData.mSongs.size() > 0) {
            // for now just load the first songs art //TODO stacked art like gmusic
            Song song = mData.mSongs.get(0);
            fetcher.loadAlbumImage(song.mArtistName, song.mAlbumName, song.mAlbumId, view);
        }
    }

    protected CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener() {
        return new CardHeader.OnClickCardHeaderPopupMenuListener() {
            @Override
            public void onMenuItemClick(BaseCard baseCard, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.card_menu_play:
                        //TODO we have the songs already just get the list from them.
                        MusicUtils.playAll(getContext(), MusicUtils.getSongListForGenre(getContext(), mData.mGenreId), 0, false);
                        break;
                    case R.id.card_menu_add_queue:
                        MusicUtils.addToQueue(getContext(), MusicUtils.getSongListForGenre(getContext(), mData.mGenreId));
                        break;
                }
            }
        };
    }

}
