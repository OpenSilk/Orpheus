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

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.cards.event.GenreCardClick;
import org.opensilk.music.ui.cards.event.GenreCardClick.Event;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.silkdagger.qualifier.ForFragment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 6/28/14.
 */
public class GenreCard extends Card {

    @Inject @ForFragment
    Bus mBus;

    @InjectView(R.id.card_title)
    protected TextView mCardTitle;
    @InjectView(R.id.card_subtitle)
    protected TextView mCardSubTitle;
    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;
    // no inject
    protected ArtworkImageView mArtwork2;

    private Genre mData;

    public GenreCard(Context context, Genre data) {
        super(context, determiteLayout(data));
        mData = data;
        init();
    }

    protected  void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new GenreCardClick(Event.OPEN, mData));
            }
        });
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        ButterKnife.inject(this, view);
        mArtwork2 = ButterKnife.findById(view, R.id.artwork_thumb2);
        onInnerViewSetup();
    }

    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.mGenreName);
        String l2 = MusicUtils.makeLabel(getContext(), R.plurals.Nalbums, mData.mAlbumNumber)
                + ", " + MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongNumber);
        mCardSubTitle.setText(l2);
        if (mArtwork2 != null) {
            ApolloUtils.execute(false, new ArtLoaderTask(mData.mGenreId, mArtwork, mArtwork2));
        } else {
            ApolloUtils.execute(false, new ArtLoaderTask(mData.mGenreId, mArtwork));
        }
    }

    @OnClick(R.id.card_overflow_button)
    public void onOverflowClicked(View v) {
        PopupMenu m = new PopupMenu(getContext(), v);
        onCreatePopupMenu(m);
        m.show();
    }

    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        m.inflate(R.menu.popup_add_to_playlist);
        m.inflate(R.menu.card_genre);
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

    public Genre getData() {
        return mData;
    }

    private static int determiteLayout(Genre genre) {
        if (genre.mAlbumNumber >= 2) {
            return R.layout.library_gridcard_dual_artwork_inner;
        } else {
            return R.layout.library_gridcard_artwork_inner;
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
                    MediaStore.Audio.Genres.Members.IS_MUSIC + "=? AND " + MediaStore.Audio.Genres.Members.TITLE + "!=?",
                    new String[] {"1", "''"},
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
