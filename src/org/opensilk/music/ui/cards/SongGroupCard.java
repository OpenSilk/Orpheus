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
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui.cards.event.GenreCardClick;
import org.opensilk.music.ui.cards.event.SongGroupCardClick;
import org.opensilk.music.ui.cards.event.SongGroupCardClick.Event;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.qualifier.ForFragment;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import butterknife.InjectView;
import it.gmariotti.cardslib.library.internal.Card;

/**
 * Created by drew on 7/10/14.
 */
public class SongGroupCard extends AbsGenericCard<LocalSongGroup> {

    @Inject @ForFragment
    Bus mBus;

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;
    // no inject
    protected ArtworkImageView mArtwork2;
    protected ArtworkImageView mArtwork3;
    protected ArtworkImageView mArtwork4;

    public SongGroupCard(Context context, LocalSongGroup data) {
        super(context, data, determiteLayout(data));
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new SongGroupCardClick(Event.OPEN, mData));
            }
        });
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.name);
        String l2 = MusicUtils.makeLabel(getContext(), R.plurals.Nalbums, mData.albumIds.length)
                + ", " + MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.songIds.length);
        mCardSubTitle.setText(l2);
        if (mData.albumIds.length > 0){
            if (mArtwork4 != null && mArtwork3 != null && mArtwork2 != null) {
                ApolloUtils.execute(false, new ArtLoaderTask(mData.albumIds, mArtwork, mArtwork2, mArtwork3, mArtwork4));
            } else if (mArtwork2 != null) {
                ApolloUtils.execute(false, new ArtLoaderTask(mData.albumIds, mArtwork, mArtwork2));
            } else {
                ApolloUtils.execute(false, new ArtLoaderTask(mData.albumIds, mArtwork));
            }
        }
    }

    @Override
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
                        mBus.post(new SongGroupCardClick(Event.PLAY_ALL, mData));
                        return true;
                    case R.id.popup_shuffle_all:
                        mBus.post(new SongGroupCardClick(Event.SHUFFLE_ALL, mData));
                        return true;
                    case R.id.popup_add_to_queue:
                        mBus.post(new SongGroupCardClick(Event.ADD_TO_QUEUE, mData));
                        return true;
                    case R.id.popup_add_to_playlist:
                        mBus.post(new SongGroupCardClick(Event.ADD_TO_QUEUE, mData));
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        throw new UnsupportedOperationException("No list for song groups");
    }

    @Override
    protected int getGridLayout() {
        return determiteLayout(mData);
    }

    private static int determiteLayout(LocalSongGroup data) {
        if (data.albumIds.length >= 4) {
            return R.layout.gridcard_artwork_quad_inner;
        } else if (data.albumIds.length >= 2) {
            return R.layout.gridcard_artwork_dual_inner;
        } else {
            return R.layout.gridcard_artwork_inner;
        }
    }

    class ArtLoaderTask extends AsyncTask<Void, Void, Set<ArtInfo>> {
        final long[] albumIds;
        final ArtworkImageView[] images;

        ArtLoaderTask(long[] albumIds, ArtworkImageView... images) {
            this.albumIds = albumIds;
            this.images = images;
        }

        @Override
        protected Set<ArtInfo> doInBackground(Void... params) {
            Set<ArtInfo> artInfos = new HashSet<>(albumIds.length);
            Cursor c = CursorHelpers.makeLocalAlbumsCursor(getContext(), albumIds);
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        ArtInfo info = CursorHelpers.makeArtInfoFromLocalAlbumCursor(c);
                        artInfos.add(info);
                    } while (c.moveToNext() && artInfos.size() <= images.length);
                }
                c.close();
            }

            return artInfos;
        }

        @Override
        protected void onPostExecute(Set<ArtInfo> artInfos) {
            if (artInfos.size() >= images.length) {
                int ii=0;
                for (ArtInfo info : artInfos) {
                    ArtworkManager.loadImage(info, images[ii++]);
                    if (ii==images.length) break;
                }
            }
        }
    }
}
