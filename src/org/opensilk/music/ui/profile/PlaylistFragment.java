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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;
import com.mobeta.android.dslv.DragSortListView;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.loader.LocalPlaylistSongLoader;
import org.opensilk.music.ui2.profile.PlaylistAdapter;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.common.dagger.DaggerInjector;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import dagger.Provides;
import rx.Subscription;
import rx.functions.Action1;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 7/11/14.
 */
public class PlaylistFragment extends ListStickyParallaxHeaderFragment implements
        DragSortListView.DropListener,
        DragSortListView.RemoveListener {

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = PlaylistFragment.class
    )
    public static class Module {
        final Playlist playlist;

        public Module(Playlist playlist) {
            this.playlist = playlist;
        }

        @Provides @Singleton @Named("playlist")
        public long providePlaylistId() {
            return playlist.mPlaylistId;
        }
    }

    @Inject OverflowHandlers.LocalSongs mAdapterOverflowHandler;
    @Inject OverflowHandlers.Playlists mPlaylistOverflowHandler;
    @Inject ArtworkRequestManager mRequestor;
    @Inject LocalPlaylistSongLoader mLoader;

    Playlist mPlaylist;

    Subscription mLoaderSubscription;
    PlaylistAdapter mAdapter;

    public static PlaylistFragment newInstance(Bundle args) {
        PlaylistFragment f = new PlaylistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPlaylist = getArguments().getParcelable(Config.EXTRA_DATA);
        ((DaggerInjector) getActivity()).getObjectGraph().plus(new Module(mPlaylist)).inject(this);

        mAdapter = new PlaylistAdapter(getActivity(), mPlaylist.mPlaylistId);
        // start the loader
        mLoaderSubscription = mLoader.getListObservable().subscribe(new Action1<List<LocalSong>>() {
            @Override
            public void call(List<LocalSong> localSongs) {
                mAdapter.addAll(localSongs);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {

            }
        });
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter = null;
        if (isSubscribed(mLoaderSubscription)) {
            mLoaderSubscription.unsubscribe();
            mLoaderSubscription = null;
        }
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
        AnimatedImageView heroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        if (mPlaylist.mAlbumIds.length == 0) {
            if (heroImage != null) (heroImage).setDefaultImage();
        } else {
            if (mPlaylist.mAlbumIds.length >= 1 && heroImage != null) {
                mRequestor.newAlbumRequest(heroImage, null, mPlaylist.mAlbumIds[0], ArtworkType.LARGE);
            }
            AnimatedImageView heroImage2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
            if (mPlaylist.mAlbumIds.length >= 2 && heroImage2 != null) {
                mRequestor.newAlbumRequest(heroImage2, null, mPlaylist.mAlbumIds[1], ArtworkType.LARGE);
            }
            AnimatedImageView heroImage3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
            if (mPlaylist.mAlbumIds.length >= 3 && heroImage3 != null) {
                mRequestor.newAlbumRequest(heroImage3, null, mPlaylist.mAlbumIds[2], ArtworkType.LARGE);
            }
            AnimatedImageView heroImage4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
            if (mPlaylist.mAlbumIds.length >= 4 && heroImage4 != null) {
                mRequestor.newAlbumRequest(heroImage4, null, mPlaylist.mAlbumIds[3], ArtworkType.LARGE);
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
            return R.layout.profile_hero4;
        } else if (mPlaylist.mAlbumIds.length >= 2) {
            return R.layout.profile_hero2;
        } else {
            return super.getHeaderLayout();
        }
    }

    /*
     * DragSort callbacks
     */

    @Override
    public void remove(final int which) {
        Song song = CursorHelpers.makeLocalSongFromCursor((Cursor) mAdapter.getItem(which));
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

    private boolean isFavorites() {
        return mPlaylist.mPlaylistId == -1;
    }

    private boolean isLastAdded() {
        return mPlaylist.mPlaylistId == -2;
    }

}
