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

import android.os.Bundle;
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
import com.andrew.apollo.model.Genre;
import com.squareup.otto.Bus;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.cards.GenreCard;
import org.opensilk.music.ui.cards.handler.AlbumCardClickHandler;
import org.opensilk.music.ui.cards.handler.GenreCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongGroupCardClickHandler;
import org.opensilk.music.ui.profile.adapter.GridAdapter;
import org.opensilk.music.ui.profile.loader.GenreGridLoader;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.util.MultipleArtworkLoaderTask;
import org.opensilk.common.dagger.qualifier.ForFragment;
import org.opensilk.silkdagger.DaggerInjector;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 7/11/14.
 */
public class GenreFragment extends ListStickyParallaxHeaderFragment implements LoaderManager.LoaderCallbacks<List<Object>> {

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = GenreFragment.class
    )
    public static class Module {

    }

    @Inject OverflowHandlers.LocalAlbums mAdapterAlbumOverflowHandler;
    @Inject OverflowHandlers.LocalSongGroups mAdapterSongGroupOverflowHandler;
    @Inject OverflowHandlers.Genres mGenreOverflowHandler;
    @Inject ArtworkRequestManager mRequestor;

    Genre mGenre;

    GridAdapter mAdapter;

    public static GenreFragment newInstance(Bundle args) {
        GenreFragment f = new GenreFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((DaggerInjector) getActivity()).getObjectGraph().plus(new Module()).inject(this);
        mGenre = getArguments().getParcelable(Config.EXTRA_DATA);
        mAdapter = new GridAdapter(getActivity(),
                mRequestor,
                mAdapterAlbumOverflowHandler,
                mAdapterSongGroupOverflowHandler);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // hero image
        ImageView heroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        if (mGenre.mAlbumIds.length == 0) {
            if (heroImage != null) ((AnimatedImageView)heroImage).setDefaultImage();
        } else {
            if (mGenre.mAlbumIds.length >= 1 && heroImage != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage, null, mGenre.mAlbumIds[0], ArtworkType.LARGE);
            }
            ImageView heroImage2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
            if (mGenre.mAlbumIds.length >= 2 && heroImage2 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage2, null, mGenre.mAlbumIds[1], ArtworkType.LARGE);
            }
            ImageView heroImage3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
            if (mGenre.mAlbumIds.length >= 3 && heroImage3 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage3, null, mGenre.mAlbumIds[2], ArtworkType.LARGE);
            }
            ImageView heroImage4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
            if (mGenre.mAlbumIds.length >= 4 && heroImage4 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage4, null, mGenre.mAlbumIds[3], ArtworkType.LARGE);
            }
        }
        // Load header text
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_title).setText(mGenre.mGenreName);
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_subtitle).setVisibility(View.GONE);
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        for (int ii : OverflowHandlers.Genres.MENUS) {
            inflater.inflate(ii, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            return mGenreOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), mGenre);
        } catch (IllegalArgumentException e) {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getHeaderLayout() {
        if (mGenre.mAlbumIds.length >= 4) {
            return R.layout.profile_hero_quad_header;
        } else if (mGenre.mAlbumIds.length >= 2) {
            return R.layout.profile_hero_dual_header;
        } else {
            return super.getHeaderLayout();
        }
    }

    @Override
    public Loader<List<Object>> onCreateLoader(int id, Bundle args) {
        return new GenreGridLoader(getActivity(), mGenre);
    }

    @Override
    public void onLoadFinished(Loader<List<Object>> loader, List<Object> data) {
        mAdapter.clear();
        mAdapter.addAll(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Object>> loader) {
        mAdapter.clear();
    }

}
