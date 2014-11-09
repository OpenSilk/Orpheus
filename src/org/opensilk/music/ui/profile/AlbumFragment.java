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
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.Config;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import com.andrew.apollo.model.LocalAlbum;
import com.squareup.otto.Bus;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.cards.AlbumCard;
import org.opensilk.music.ui.cards.handler.AlbumCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.profile.adapter.SongCollectionAdapter;
import org.opensilk.music.ui.profile.loader.AlbumSongLoader;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.util.CursorHelpers;
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
 * Created by drew on 7/10/14.
 */
public class AlbumFragment extends ListStickyParallaxHeaderFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = AlbumFragment.class
    )
    public static class Module {

    }

    @Inject OverflowHandlers.LocalSongs mAdapterOverflowHandler;
    @Inject OverflowHandlers.LocalAlbums mAlbumsOverflowHandler;
    @Inject ArtworkRequestManager mRequestor;

    LocalAlbum mAlbum;

    SongCollectionAdapter mAdapter;

    public static AlbumFragment newInstance(Bundle args) {
        AlbumFragment f = new AlbumFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((DaggerInjector) getActivity()).getObjectGraph().plus(new Module()).inject(this);
        mAlbum = getArguments().getParcelable(Config.EXTRA_DATA);
        mAdapter = new SongCollectionAdapter(getActivity(),
                mAdapterOverflowHandler,
                mRequestor,
                true,
                Uris.LOCAL_ALBUM_SONGS,
                Projections.LOCAL_SONG,
                Selections.LOCAL_ALBUM_SONGS,
                SelectionArgs.LOCAL_ALBUM_SONGS(mAlbum.albumId),
                SortOrder.LOCAL_ALBUM_SONGS);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // hero image
        AnimatedImageView heroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        mRequestor.newAlbumRequest(heroImage, mPaletteObserver,
                new ArtInfo(mAlbum.artistName, mAlbum.name, mAlbum.artworkUri), ArtworkType.LARGE);
        // Load header text
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_title).setText(mAlbum.name);
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_subtitle).setText(mAlbum.artistName);
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        for (int ii : OverflowHandlers.LocalAlbums.MENUS) {
            inflater.inflate(ii, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            return mAlbumsOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), mAlbum);
        } catch (IllegalArgumentException e) {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getListLayout() {
        return R.layout.profile_list_frame;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AlbumSongLoader(getActivity(), mAlbum.albumId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

}
