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

package org.opensilk.music.ui.profile;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.TextView;

import com.andrew.apollo.Config;
import org.opensilk.music.R;
import com.andrew.apollo.model.Playlist;
import com.mobeta.android.dslv.DragSortListView;
import com.squareup.otto.Bus;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.ui.cards.PlaylistCard;
import org.opensilk.music.ui.cards.SongPlaylistCard;
import org.opensilk.music.ui.cards.handler.PlaylistCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.profile.adapter.PlaylistAdapter;
import org.opensilk.music.ui.profile.loader.PlaylistSongLoader;
import org.opensilk.music.util.MultipleArtworkLoaderTask;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
import org.opensilk.common.dagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 7/11/14.
 */
public class PlaylistFragment extends ListStickyParallaxHeaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        DragSortListView.DropListener,
        DragSortListView.RemoveListener {

    protected ArtworkImageView mHeroImage;
    protected ArtworkImageView mHeroImage2;
    protected ArtworkImageView mHeroImage3;
    protected ArtworkImageView mHeroImage4;
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mHeaderOverflow;

    private Playlist mPlaylist;

    protected PlaylistAdapter mAdapter;
    @Inject @ForFragment
    protected Bus mBus;

    public static PlaylistFragment newInstance(Bundle args) {
        PlaylistFragment f = new PlaylistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlaylist = getArguments().getParcelable(Config.EXTRA_DATA);
        final Uri uri;
        final String[] projection;
        final String selection;
        final String[] selectionArgs;
        final String sortOrder;
        if (isLastAdded()) {
            uri = Uris.EXTERNAL_MEDIASTORE_MEDIA;
            projection = Projections.LOCAL_SONG;
            selection = Selections.LAST_ADDED;
            selectionArgs = SelectionArgs.LAST_ADDED();
            sortOrder = SortOrder.LAST_ADDED;
        } else { //User generated playlist
            uri = Uris.PLAYLIST(mPlaylist.mPlaylistId);
            projection = Projections.PLAYLIST_SONGS;
            selection = Selections.LOCAL_SONG;
            selectionArgs = SelectionArgs.LOCAL_SONG;
            sortOrder = SortOrder.PLAYLIST_SONGS;
        }
        mAdapter = new PlaylistAdapter(getActivity(), this,
                uri, projection, selection, selectionArgs, sortOrder,
                mPlaylist.mPlaylistId);
        //We have to set this manually since we arent using CardListView
        mAdapter.setRowLayoutId(R.layout.list_card_dragsort_layout);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
        registerHandlers();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (isLastAdded()) {
            // last added arent sortable
            ((DragSortListView) mList).setDragEnabled(false);
        } else {
            // Set the drop listener
            ((DragSortListView) mList).setDropListener(this);
            // Set the swipe to remove listener
            ((DragSortListView) mList).setRemoveListener(this);
        }
        // hero image
        mHeroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        mHeroImage.setPaletteListener(this);
        // Load header images
        mHeroImage2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
        mHeroImage3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
        mHeroImage4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
        if (mHeroImage4 != null && mHeroImage3 != null && mHeroImage2 != null) {
            new MultipleArtworkLoaderTask(getActivity(), mPlaylist.mAlbumIds, mHeroImage, mHeroImage2, mHeroImage3, mHeroImage4).execute();
        } else if (mHeroImage2 != null) {
            new MultipleArtworkLoaderTask(getActivity(), mPlaylist.mAlbumIds, mHeroImage, mHeroImage2).execute();
        } else {
            new MultipleArtworkLoaderTask(getActivity(), mPlaylist.mAlbumIds, mHeroImage).execute();
        }
        // Load header text
        mInfoTitle = ButterKnife.findById(mStickyHeader, R.id.info_title);
        mInfoTitle.setText(mPlaylist.mPlaylistName);
        mInfoSubTitle = ButterKnife.findById(mStickyHeader, R.id.info_subtitle);
        mInfoSubTitle.setVisibility(View.GONE);
        //overflow
        mHeaderOverflow = ButterKnife.findById(mStickyHeader, R.id.profile_header_overflow);
        final PlaylistCard playlistCard = new PlaylistCard(getActivity(), mPlaylist);
        inject(playlistCard);
        mHeaderOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistCard.onOverflowClicked(v);
            }
        });
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterHandlers();
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ProfileModule(),
        };
    }

    @Override
    protected int getListLayout() {
        return R.layout.profile_dragsortlist_frame;
    }

    @Override
    protected int getHeaderLayout() {
        if (mPlaylist.mAlbumNumber < 2) {
            return super.getHeaderLayout();
        } else if (mPlaylist.mAlbumNumber < 4) {
            return R.layout.profile_hero_dual_header;
        } else {
            return R.layout.profile_hero_quad_header;
        }
    }

    /*
     * DragSort callbacks
     */

    @Override
    public void remove(final int which) {
        Song song = ((SongPlaylistCard) mAdapter.getItem(which)).getData();
        if (!isFavorites()) {
            final Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", mPlaylist.mPlaylistId);
            getActivity().getContentResolver().delete(uri,
                    MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?",
                    new String[]{song.identity});
        }
    }

    @Override
    public void drop(final int from, final int to) {
        if (!isFavorites()) {
            MediaStore.Audio.Playlists.Members.moveItem(getActivity().getContentResolver(),
                    mPlaylist.mPlaylistId, from, to);
        }
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new PlaylistSongLoader(getActivity(), mPlaylist.mPlaylistId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    private boolean isFavorites() {
        return mPlaylist.mPlaylistId == -1;
    }

    private boolean isLastAdded() {
        return mPlaylist.mPlaylistId == -2;
    }

    private PlaylistCardClickHandler mPlaylistHandler;
    private SongCardClickHandler mSongHandler;

    private void registerHandlers() {
        mPlaylistHandler = getObjectGraph().get(PlaylistCardClickHandler.class);
        mSongHandler = getObjectGraph().get(SongCardClickHandler.class);
        mBus.register(mPlaylistHandler);
        mBus.register(mSongHandler);
    }

    private void unregisterHandlers() {
        mBus.unregister(mPlaylistHandler);
        mBus.unregister(mSongHandler);
    }
}
