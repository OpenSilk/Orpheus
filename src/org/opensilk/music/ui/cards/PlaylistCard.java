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
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Bus;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui.cards.event.PlaylistCardClick;
import org.opensilk.music.ui.cards.event.PlaylistCardClick.Event;
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
 * Created by drew on 6/30/14.
 */
public class PlaylistCard extends AbsGenericCard<Playlist> {

    @Inject @ForFragment
    Bus mBus;

    @InjectView(R.id.artwork_thumb)
    protected ArtworkImageView mArtwork;
    // no inject
    protected ArtworkImageView mArtwork2;

    public PlaylistCard(Context context, Playlist data) {
        super(context, data, determiteLayout(data));
    }

    @Override
    protected void init() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                mBus.post(new PlaylistCardClick(Event.OPEN, mData));
            }
        });
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        mArtwork2 = ButterKnife.findById(view, R.id.artwork_thumb2);
        super.setupInnerViewElements(parent, view);
    }

    @Override
    protected void onInnerViewSetup() {
        mCardTitle.setText(mData.mPlaylistName);
        mCardSubTitle.setText(MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongNumber));
        if (mData.mAlbumNumber > 0) {
            if (mArtwork2 != null) {
                ApolloUtils.execute(false, new ArtLoaderTask(mData.mPlaylistId, mArtwork, mArtwork2));
            } else {
                ApolloUtils.execute(false, new ArtLoaderTask(mData.mPlaylistId, mArtwork));
            }
        }
    }

    @Override
    protected void onCreatePopupMenu(PopupMenu m) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
        if (mData.mPlaylistId != -2) {
            // cant rename or delete last added
            m.inflate(R.menu.popup_rename);
            m.inflate(R.menu.popup_delete);
        }
        m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.popup_play_all:
                        mBus.post(new PlaylistCardClick(Event.PLAY_ALL, mData));
                        break;
                    case R.id.popup_shuffle_all:
                        mBus.post(new PlaylistCardClick(Event.SHUFFLE_ALL, mData));
                        break;
                    case R.id.popup_add_to_queue:
                        mBus.post(new PlaylistCardClick(Event.ADD_TO_QUEUE, mData));
                        break;
                    case R.id.popup_rename:
                        mBus.post(new PlaylistCardClick(Event.RENAME, mData));
                        break;
                    case R.id.popup_delete:
                        mBus.post(new PlaylistCardClick(Event.DELETE, mData));
                        break;
                }
                return false;
            }
        });
    }

    @Override
    protected int getListLayout() {
        throw new UnsupportedOperationException("No list for playlists");
    }

    @Override
    protected int getGridLayout() {
        return determiteLayout(mData);
    }

    private static int determiteLayout(Playlist plist) {
        if (plist.mAlbumNumber >= 2) {
            return R.layout.gridcard_artwork_dual_inner;
        } else {
            return R.layout.gridcard_artwork_inner;
        }
    }

    class ArtLoaderTask extends AsyncTask<Void, Void, Set<ArtInfo>> {
        final ArtworkImageView[] views;
        final long playlistId;

        ArtLoaderTask(long playlistId, ArtworkImageView... imageViews) {
            this.views = imageViews;
            this.playlistId = playlistId;
        }

        @Override
        protected Set<ArtInfo> doInBackground(Void... params) {

            Set<ArtInfo> artInfos = new HashSet<>();
            // We have to query for the song count
            final Cursor playlistSongs;
            if (playlistId == -2) { //Last added
                playlistSongs = CursorHelpers.makeLastAddedCursor(getContext());
            } else { // user
                playlistSongs = getContext().getContentResolver().query(
                        MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                        new String[]{
                                MediaStore.Audio.Media.ARTIST, //TODO this should really be albumartist
                                MediaStore.Audio.Media.ALBUM,
                                MediaStore.Audio.Media.ALBUM_ID,
                        },
                        Selections.LOCAL_SONG,
                        SelectionArgs.LOCAL_SONG,
                        MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER
                );
            }
            if (playlistSongs != null && playlistSongs.moveToFirst()) {
                do {
                    String artist = playlistSongs.getString(playlistSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
                    String album = playlistSongs.getString(playlistSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
                    long albumId = playlistSongs.getLong(playlistSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
                    Uri artworkUri = CursorHelpers.generateArtworkUri(albumId);
                    artInfos.add(new ArtInfo(artist, album, artworkUri));
                } while (playlistSongs.moveToNext() && artInfos.size() <= views.length);
            }
            if (playlistSongs != null) {
                playlistSongs.close();
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
