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
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui.cards.event.GenreCardClick;
import org.opensilk.music.ui.cards.event.GenreCardClick.Event;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.silkdagger.qualifier.ForFragment;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/28/14.
 */
public class GenreCard extends AbsGenericCard<Genre> {

    @Inject @ForFragment
    Bus mBus;

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;
    // no inject
    protected ArtworkImageView mArtwork2;
    protected ArtworkImageView mArtwork3;
    protected ArtworkImageView mArtwork4;

    public GenreCard(Context context, Genre data) {
        super(context, data, determiteLayout(data));
    }

    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new GenreCardClick(Event.OPEN, mData));
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

    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.mGenreName);
        String l2 = MusicUtils.makeLabel(getContext(), R.plurals.Nalbums, mData.mAlbumNumber)
                + ", " + MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongNumber);
        mCardSubTitle.setText(l2);
        if (mData.mAlbumNumber > 0){
            if (mArtwork4 != null && mArtwork3 != null && mArtwork2 != null) {
                ApolloUtils.execute(false, new ArtLoaderTask(mData.mGenreId, mArtwork, mArtwork2, mArtwork3, mArtwork4));
            } else if (mArtwork2 != null) {
                ApolloUtils.execute(false, new ArtLoaderTask(mData.mGenreId, mArtwork, mArtwork2));
            } else {
                ApolloUtils.execute(false, new ArtLoaderTask(mData.mGenreId, mArtwork));
            }
        }
    }

    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        m.inflate(R.menu.popup_add_to_playlist);
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.popup_play_all:
                        mBus.post(new GenreCardClick(Event.PLAY_ALL, mData));
                        return true;
                    case R.id.popup_shuffle_all:
                        mBus.post(new GenreCardClick(Event.SHUFFLE_ALL, mData));
                        return true;
                    case R.id.popup_add_to_queue:
                        mBus.post(new GenreCardClick(Event.ADD_TO_QUEUE, mData));
                        return true;
                    case R.id.popup_add_to_playlist:
                        mBus.post(new GenreCardClick(Event.ADD_TO_QUEUE, mData));
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        throw new UnsupportedOperationException("No list for genres");
    }

    @Override
    protected int getGridLayout() {
        return determiteLayout(mData);
    }

    private static int determiteLayout(Genre genre) {
        if (genre.mAlbumNumber >= 4) {
            return R.layout.gridcard_artwork_quad_inner;
        } else if (genre.mAlbumNumber >= 2) {
            return R.layout.gridcard_artwork_dual_inner;
        } else {
            return R.layout.gridcard_artwork_inner;
        }
    }

    class ArtLoaderTask extends AsyncTask<Void, Void, Set<ArtInfo>> {
        final ArtworkImageView[] views;
        final long genreId;

        ArtLoaderTask(long genreId, ArtworkImageView... imageViews) {
            this.views = imageViews;
            this.genreId = genreId;
        }

        @Override
        protected Set<ArtInfo> doInBackground(Void... params) {
            Set<ArtInfo> artInfos = new HashSet<>();
            final Cursor genreSongs = getContext().getContentResolver().query(
                    MediaStore.Audio.Genres.Members.getContentUri("external", genreId),
                    new String[]{
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.ALBUM_ID,
                    },
                    Selections.LOCAL_SONG,
                    SelectionArgs.LOCAL_SONG,
                    MediaStore.Audio.Genres.Members.DEFAULT_SORT_ORDER);
            if (genreSongs != null && genreSongs.moveToFirst()) {
                do {
                    String artist = genreSongs.getString(0);
                    String album = genreSongs.getString(1);
                    long albumId = genreSongs.getLong(2);
                    Uri artworkUri = CursorHelpers.generateArtworkUri(albumId);
                    artInfos.add(new ArtInfo(artist, album, artworkUri));
                } while (genreSongs.moveToNext() && artInfos.size() <= views.length );
            }
            if (genreSongs != null) {
                genreSongs.close();
            }
            return artInfos;
        }

        @Override
        protected void onPostExecute(Set<ArtInfo> artInfos) {
            if (artInfos.size() >= views.length) {
                int ii = 0;
                for (ArtInfo a : artInfos) {
                    ArtworkManager.loadImage(a, views[ii++]);
                    if (ii == views.length) break;
                }
            }
        }
    }


}
