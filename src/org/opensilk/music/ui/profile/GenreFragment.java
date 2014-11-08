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
import android.view.View;
import android.widget.TextView;

import com.andrew.apollo.Config;
import org.opensilk.music.R;
import com.andrew.apollo.model.Genre;
import com.squareup.otto.Bus;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.ui.cards.GenreCard;
import org.opensilk.music.ui.cards.handler.AlbumCardClickHandler;
import org.opensilk.music.ui.cards.handler.GenreCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongGroupCardClickHandler;
import org.opensilk.music.ui.profile.adapter.GridAdapter;
import org.opensilk.music.ui.profile.loader.GenreGridLoader;
import org.opensilk.music.util.MultipleArtworkLoaderTask;
import org.opensilk.common.dagger.qualifier.ForFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 7/11/14.
 */
public class GenreFragment extends ListStickyParallaxHeaderFragment implements LoaderManager.LoaderCallbacks<List<Object>> {

    protected ArtworkImageView mHeroImage;
    protected ArtworkImageView mHeroImage2;
    protected ArtworkImageView mHeroImage3;
    protected ArtworkImageView mHeroImage4;
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mHeaderOverflow;

    private Genre mGenre;

    @Inject
    GridAdapter mAdapter;

    public static GenreFragment newInstance(Bundle args) {
        GenreFragment f = new GenreFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGenre = getArguments().getParcelable(Config.EXTRA_DATA);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // hero image
        mHeroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        mHeroImage.setPaletteListener(this);
        // Load header images
        mHeroImage2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
        mHeroImage3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
        mHeroImage4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
        if (mHeroImage4 != null && mHeroImage3 != null && mHeroImage2 != null) {
            new MultipleArtworkLoaderTask(getActivity(), mGenre.mAlbumIds, mHeroImage, mHeroImage2, mHeroImage3, mHeroImage4).execute();
        } else if (mHeroImage2 != null) {
            new MultipleArtworkLoaderTask(getActivity(), mGenre.mAlbumIds, mHeroImage, mHeroImage2).execute();
        } else {
            new MultipleArtworkLoaderTask(getActivity(), mGenre.mAlbumIds, mHeroImage).execute();
        }
        // Load header text
        mInfoTitle = ButterKnife.findById(mStickyHeader, R.id.info_title);
        mInfoTitle.setText(mGenre.mGenreName);
        mInfoSubTitle = ButterKnife.findById(mStickyHeader, R.id.info_subtitle);
        mInfoSubTitle.setVisibility(View.GONE);
        //overflow
        mHeaderOverflow = ButterKnife.findById(mStickyHeader, R.id.profile_header_overflow);
        final GenreCard genreCard = new GenreCard(getActivity(), mGenre);
        inject(genreCard);
        mHeaderOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                genreCard.onOverflowClicked(v);
            }
        });
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ProfileModule(),
        };
    }

    @Override
    protected int getHeaderLayout() {
        if (mGenre.mAlbumNumber < 2) {
            return super.getHeaderLayout();
        } else if (mGenre.mAlbumNumber < 4) {
            return R.layout.profile_hero_dual_header;
        } else {
            return R.layout.profile_hero_quad_header;
        }
    }

    @Override
    public Loader<List<Object>> onCreateLoader(int id, Bundle args) {
        return new GenreGridLoader(getActivity(), mGenre);
    }

    @Override
    public void onLoadFinished(Loader<List<Object>> loader, List<Object> data) {
        mAdapter.addAll(data);
    }

    @Override
    public void onLoaderReset(Loader<List<Object>> loader) {
        mAdapter.clear();
    }

}
