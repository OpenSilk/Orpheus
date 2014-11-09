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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apollo.Config;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import com.andrew.apollo.model.Playlist;
import com.mobeta.android.dslv.DragSortListView;
import com.squareup.otto.Bus;

import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.cards.PlaylistCard;
import org.opensilk.music.ui.cards.SongPlaylistCard;
import org.opensilk.music.ui.cards.handler.PlaylistCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.profile.adapter.PlaylistAdapter;
import org.opensilk.music.ui.profile.loader.PlaylistSongLoader;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.util.MultipleArtworkLoaderTask;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
import org.opensilk.common.dagger.qualifier.ForFragment;
import org.opensilk.silkdagger.DaggerInjector;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 7/11/14.
 */
public class PlaylistFragment extends ListStickyParallaxHeaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        DragSortListView.DropListener,
        DragSortListView.RemoveListener {

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = PlaylistFragment.class
    )
    public static class Module {

    }

    @Inject OverflowHandlers.LocalSongs mAdapterOverflowHandler;
    @Inject OverflowHandlers.Playlists mPlaylistOverflowHandler;
    @Inject ArtworkRequestManager mRequestor;

    Playlist mPlaylist;

    PlaylistAdapter mAdapter;

    public static PlaylistFragment newInstance(Bundle args) {
        PlaylistFragment f = new PlaylistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((DaggerInjector) getActivity()).getObjectGraph().plus(new Module()).inject(this);
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
        mAdapter = new PlaylistAdapter(getActivity(),
                mAdapterOverflowHandler,
                mRequestor,
                uri, projection, selection, selectionArgs, sortOrder,
                mPlaylist.mPlaylistId);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
        setHasOptionsMenu(true);
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
        ImageView heroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        if (mPlaylist.mAlbumIds.length == 0) {
            if (heroImage != null) ((AnimatedImageView)heroImage).setDefaultImage();
        } else {
            if (mPlaylist.mAlbumIds.length >= 1 && heroImage != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage, null, mPlaylist.mAlbumIds[0], ArtworkType.LARGE);
            }
            ImageView heroImage2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
            if (mPlaylist.mAlbumIds.length >= 2 && heroImage2 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage2, null, mPlaylist.mAlbumIds[1], ArtworkType.LARGE);
            }
            ImageView heroImage3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
            if (mPlaylist.mAlbumIds.length >= 3 && heroImage3 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage3, null, mPlaylist.mAlbumIds[2], ArtworkType.LARGE);
            }
            ImageView heroImage4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
            if (mPlaylist.mAlbumIds.length >= 4 && heroImage4 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage4, null, mPlaylist.mAlbumIds[3], ArtworkType.LARGE);
            }
        }
        // Load header text
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_title).setText(mPlaylist.mPlaylistName);
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_subtitle).setVisibility(View.GONE);
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        for (int ii : OverflowHandlers.Playlists.MENUS_COMMON) {
            inflater.inflate(ii, menu);
        }
        if (!isLastAdded()) {
            for (int ii : OverflowHandlers.Playlists.MENUS_USER) {
                inflater.inflate(ii, menu);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            return mPlaylistOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), mPlaylist);
        } catch (IllegalArgumentException e) {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getListLayout() {
        return R.layout.profile_dragsortlist_frame;
    }

    @Override
    protected int getHeaderLayout() {
        if (mPlaylist.mAlbumIds.length >= 4) {
            return R.layout.profile_hero_quad_header;
        } else if (mPlaylist.mAlbumIds.length >= 2) {
            return R.layout.profile_hero_dual_header;
        } else {
            return super.getHeaderLayout();
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

}
