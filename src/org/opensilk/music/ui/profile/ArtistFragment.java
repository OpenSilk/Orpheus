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
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.model.LocalArtist;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.profile.adapter.GridAdapter;
import org.opensilk.music.ui.profile.loader.ArtistGridLoader;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.common.dagger.DaggerInjector;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 7/9/14.
 */
public class ArtistFragment extends ListStickyParallaxHeaderFragment implements LoaderManager.LoaderCallbacks<List<Object>> {

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = ArtistFragment.class
    )
    public static class Module {

    }

    @Inject OverflowHandlers.LocalAlbums mAdapterAlbumOverflowHandler;
    @Inject OverflowHandlers.LocalSongGroups mAdapterSongGroupOverflowHandler;
    @Inject OverflowHandlers.LocalArtists mArtistsOverflowHandler;
    @Inject ArtworkRequestManager mRequestor;

    LocalArtist mArtist;

    GridAdapter mAdapter;

    public static ArtistFragment newInstance(Bundle args) {
        ArtistFragment f = new ArtistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((DaggerInjector) getActivity()).getObjectGraph().plus(new Module()).inject(this);
        mArtist = getArguments().getParcelable(Config.EXTRA_DATA);
        mAdapter = new GridAdapter(getActivity(),
                mRequestor,
                mAdapterAlbumOverflowHandler,
                mAdapterSongGroupOverflowHandler);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mAdapter = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // hero image
        AnimatedImageView heroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        mRequestor.newArtistRequest(heroImage, mPaletteObserver,
                new ArtInfo(mArtist.name, null, null), ArtworkType.LARGE);
        // Load header text
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_title).setText(mArtist.name);
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_subtitle).setVisibility(View.GONE);
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        for (int ii : OverflowHandlers.LocalArtists.MENUS) {
            inflater.inflate(ii, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            return mArtistsOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), mArtist);
        } catch (IllegalArgumentException e) {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<List<Object>> onCreateLoader(int id, Bundle args) {
        return new ArtistGridLoader(getActivity(), mArtist);
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
