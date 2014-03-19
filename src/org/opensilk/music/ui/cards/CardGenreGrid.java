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
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.model.ArtInfo;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.loaders.Projections;

import java.util.ArrayList;
import java.util.List;

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
                NavUtils.openGenreProfile(getContext(), mData);
            }
        });
    }

    @Override
    protected void initHeader() {
        final CardHeaderGrid header = new CardHeaderGrid(getContext());
        header.setButtonOverflowVisible(true);
        header.setTitle(mData.mGenreName);
        header.setLineTwo(MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongNumber));
        header.setPopupMenu(R.menu.card_genre, getNewHeaderPopupMenuListener());
        addCardHeader(header);
    }

    @Override
    protected void loadThumbnail(ImageFetcher fetcher, ImageView view) {
        // Wrap call in a async task since we are hitting the mediastore
        ApolloUtils.execute(false, new ArtLoaderTask(fetcher, view, mData.mGenreId));
    }

    protected CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener() {
        return new CardHeader.OnClickCardHeaderPopupMenuListener() {
            @Override
            public void onMenuItemClick(BaseCard baseCard, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.card_menu_play:
                        MusicUtils.playAll(getContext(), MusicUtils.getSongListForGenre(getContext(), mData.mGenreId), 0, false);
                        break;
                    case R.id.card_menu_add_queue:
                        MusicUtils.addToQueue(getContext(), MusicUtils.getSongListForGenre(getContext(), mData.mGenreId));
                        break;
                }
            }
        };
    }

    private class ArtLoaderTask extends AsyncTask<Void, Void, List<ArtInfo>> {
        final ImageFetcher fetcher;
        final ImageView view;
        final long genreId;

        ArtLoaderTask(ImageFetcher fetcher, ImageView view, long genreId) {
            this.fetcher = fetcher;
            this.view = view;
            this.genreId = genreId;
        }

        @Override
        protected List<ArtInfo> doInBackground(Void... params) {
            List<ArtInfo> artInfos = new ArrayList<ArtInfo>(1);
            final Cursor genreSongs = getContext().getContentResolver().query(
                    MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                    Projections.SONG,
                    MediaStore.Audio.Genres.Members.IS_MUSIC + "=? AND " + MediaStore.Audio.Genres.Members.TITLE + "!=?",
                    new String[] {"1", "''"}, MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);
            if (genreSongs != null && genreSongs.moveToFirst()) {
                // For now we only load one song
                String artist = genreSongs.getString(genreSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
                String album = genreSongs.getString(genreSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
                long albumId = genreSongs.getLong(genreSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
                artInfos.add(new ArtInfo(artist, album, albumId));
            }
            if (genreSongs != null) {
                genreSongs.close();
            }
            return artInfos;
        }

        @Override
        protected void onPostExecute(List<ArtInfo> artInfos) {
            for (ArtInfo info: artInfos) {
                fetcher.loadAlbumImage(info.mArtistName, info.mAlbumName, info.mAlbumId, view);
                break;// only loading the first
            }
        }
    }

}
