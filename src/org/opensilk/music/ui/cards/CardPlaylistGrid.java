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

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.andrew.apollo.R;
import com.andrew.apollo.menu.RenamePlaylist;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.util.Projections;

import java.util.ArrayList;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.internal.base.BaseCard;

/**
 * Created by drew on 2/16/14.
 */
public class CardPlaylistGrid extends CardBaseThumb<Playlist> {

    public CardPlaylistGrid(Context context, Playlist data) {
        super(context, data);
    }

    public CardPlaylistGrid(Context context, Playlist data, int innerLayout) {
        super(context, data, innerLayout);
    }

    @Override
    protected void initContent() {
        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                NavUtils.openPlaylistProfile(getContext(), mData);
            }
        });
    }

    @Override
    protected void initHeader() {
        final CardHeaderGrid header = new CardHeaderGrid(getContext());
        header.setButtonOverflowVisible(true);
        header.setTitle(mData.mPlaylistName);
        header.setLineTwo(MusicUtils.makeLabel(getContext(), R.plurals.Nsongs, mData.mSongNumber));
        header.setPopupMenu(R.menu.card_playlist, getNewHeaderPopupMenuListener());
        header.setPopupMenuPrepareListener(getNewOnPrepareListener());
        addCardHeader(header);
    }

    @Override
    protected void loadThumbnail(ArtworkImageView view) {
        if (mData.mSongNumber > 0) {
            // Wrap call in a async task since we are hitting the mediastore
            ApolloUtils.execute(false, new ArtLoaderTask(view, mData.mPlaylistId));
        }
    }

    protected CardHeader.OnClickCardHeaderPopupMenuListener getNewHeaderPopupMenuListener() {
        return new CardHeader.OnClickCardHeaderPopupMenuListener() {
            @Override
            public void onMenuItemClick(BaseCard baseCard, MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.card_menu_play:
                        if (mData.mPlaylistId == -1) {
                            MusicUtils.playFavorites(getContext(), false);
                        } else if (mData.mPlaylistId == -2) {
                            MusicUtils.playLastAdded(getContext(), false);
                        } else {
                            MusicUtils.playPlaylist(getContext(), mData.mPlaylistId, false);
                        }
                        break;
                    case R.id.card_menu_shuffle:
                        if (mData.mPlaylistId == -1) {
                            MusicUtils.playFavorites(getContext(), true);
                        } else if (mData.mPlaylistId == -2) {
                            MusicUtils.playLastAdded(getContext(), true);
                        } else {
                            MusicUtils.playPlaylist(getContext(), mData.mPlaylistId, true);
                        }
                        break;
                    case R.id.card_menu_add_queue:
                        long[] list = null;
                        if (mData.mPlaylistId == -1) {
                            list = MusicUtils.getSongListForFavorites(getContext());
                        } else if (mData.mPlaylistId == -2) {
                            list = MusicUtils.getSongListForLastAdded(getContext());
                        } else {
                            list = MusicUtils.getSongListForPlaylist(getContext(),
                                    mData.mPlaylistId);
                        }
                        MusicUtils.addToQueue(getContext(), list);
                        break;
                    case R.id.card_menu_rename:
                        RenamePlaylist.getInstance(mData.mPlaylistId).show(
                                ((FragmentActivity) getContext()).getSupportFragmentManager(),
                                "RenameDialog");
                        break;
                    case R.id.card_menu_delete:
                        new AlertDialog.Builder(getContext())
                                .setTitle(getContext().getString(R.string.delete_dialog_title, mData.mPlaylistName))
                                .setPositiveButton(R.string.context_menu_delete, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                                        final Uri mUri = ContentUris.withAppendedId(
                                                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                                mData.mPlaylistId);
                                        getContext().getContentResolver().delete(mUri, null, null);
                                        MusicUtils.refresh();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .setMessage(R.string.cannot_be_undone)
                                .create()
                                .show();
                        break;
                }
            }
        };
    }

    protected CardHeader.OnPrepareCardHeaderPopupMenuListener getNewOnPrepareListener() {
        return new CardHeader.OnPrepareCardHeaderPopupMenuListener() {
            @Override
            public boolean onPreparePopupMenu(BaseCard baseCard, PopupMenu popupMenu) {
                if (mData.mPlaylistId == -2) {
                    // cant rename or delete last added
                    popupMenu.getMenu().removeItem(R.id.card_menu_rename);
                    popupMenu.getMenu().removeItem(R.id.card_menu_delete);
                }
                return true;
            }
        };
    }

    private class ArtLoaderTask extends AsyncTask<Void, Void, List<ArtInfo>> {
        final ArtworkImageView view;
        final long playlistId;

        ArtLoaderTask(ArtworkImageView view, long playlistId) {
            this.view = view;
            this.playlistId = playlistId;
        }

        @Override
        protected List<ArtInfo> doInBackground(Void... params) {
            List<ArtInfo> artInfos = new ArrayList<ArtInfo>(1);
            // We have to query for the song count
            final Cursor playlistSongs;
            if (playlistId == -2) { //Last added
                playlistSongs = CursorHelpers.makeLastAddedCursor(getContext());
            } else { // user
                playlistSongs = getContext().getContentResolver().query(
                        MediaStore.Audio.Playlists.Members.getContentUri("external", playlistId),
                        Projections.LOCAL_SONG,
                        MediaStore.Audio.Playlists.Members.IS_MUSIC + "=? AND " + MediaStore.Audio.Playlists.Members.TITLE + "!=?",
                        new String[] {"1", "''"},
                        MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER
                );
            }
            if (playlistSongs != null && playlistSongs.moveToFirst()) {
                do {
                    String artist = playlistSongs.getString(playlistSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST));
                    String album = playlistSongs.getString(playlistSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM));
                    long albumId = playlistSongs.getLong(playlistSongs.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID));
                    Uri artworkUri = ContentUris.withAppendedId(CursorHelpers.ARTWORK_URI, albumId);
                    artInfos.add(new ArtInfo(artist, album, artworkUri));
                    // For now we only load one song
                    break;
                } while (playlistSongs.moveToNext());
            }
            if (playlistSongs != null) {
                playlistSongs.close();
            }
            return artInfos;
        }

        @Override
        protected void onPostExecute(List<ArtInfo> artInfos) {
            for (ArtInfo info: artInfos) {
                ArtworkManager.loadImage(info, view);
                break;// only loading the first
            }
        }
    }

}
