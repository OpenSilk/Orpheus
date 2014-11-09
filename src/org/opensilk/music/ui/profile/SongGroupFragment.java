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
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apollo.Config;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import com.andrew.apollo.model.LocalSongGroup;
import com.squareup.otto.Bus;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.cards.SongGroupCard;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongGroupCardClickHandler;
import org.opensilk.music.ui.profile.adapter.SongCollectionAdapter;
import org.opensilk.music.ui.profile.loader.SongGroupLoader;
import org.opensilk.music.ui2.ProfileActivity;
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
public class SongGroupFragment extends ListStickyParallaxHeaderFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = SongGroupFragment.class
    )
    public static class Module {

    }

    @Inject OverflowHandlers.LocalSongs mAdapterOverflowHandler;
    @Inject ArtworkRequestManager mRequestor;

    LocalSongGroup mSongGroup;

    SongCollectionAdapter mAdapter;

    public static SongGroupFragment newInstance(Bundle args) {
        SongGroupFragment f = new SongGroupFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((DaggerInjector) getActivity()).getObjectGraph().plus(new Module()).inject(this);
        mSongGroup = getArguments().getParcelable(Config.EXTRA_DATA);
        mAdapter = new SongCollectionAdapter(getActivity(),
                mAdapterOverflowHandler,
                mRequestor,
                false,
                Uris.EXTERNAL_MEDIASTORE_MEDIA,
                Projections.LOCAL_SONG,
                Selections.SONG_GROUP(mSongGroup.songIds),
                SelectionArgs.SONG_GROUP,
                SortOrder.SONG_GROUP);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // hero image
        ImageView heroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        if (mSongGroup.albumIds.length == 0) {
            if (heroImage != null) ((AnimatedImageView)heroImage).setDefaultImage();
        } else {
            if (mSongGroup.albumIds.length >= 1 && heroImage != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage, null, mSongGroup.albumIds[0], ArtworkType.LARGE);
            }
            ImageView heroImage2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
            if (mSongGroup.albumIds.length >= 2 && heroImage2 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage2, null, mSongGroup.albumIds[1], ArtworkType.LARGE);
            }
            ImageView heroImage3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
            if (mSongGroup.albumIds.length >= 3 && heroImage3 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage3, null, mSongGroup.albumIds[2], ArtworkType.LARGE);
            }
            ImageView heroImage4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
            if (mSongGroup.albumIds.length >= 4 && heroImage4 != null) {
                mRequestor.newAlbumRequest((AnimatedImageView)heroImage4, null, mSongGroup.albumIds[3], ArtworkType.LARGE);
            }
        }
        // Load header text
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_title).setText(mSongGroup.parentName);
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_subtitle).setText(mSongGroup.name);
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    protected int getListLayout() {
        return R.layout.profile_list_frame;
    }

    @Override
    protected int getHeaderLayout() {
        if (mSongGroup.albumIds.length >= 4) {
            return R.layout.profile_hero_quad_header;
        } else if (mSongGroup.albumIds.length >= 2) {
            return R.layout.profile_hero_dual_header;
        } else {
            return super.getHeaderLayout();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new SongGroupLoader(getActivity(), mSongGroup.songIds);
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
