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
import com.andrew.apollo.model.LocalArtist;
import com.squareup.otto.Bus;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui.cards.ArtistCard;
import org.opensilk.music.ui.cards.handler.AlbumCardClickHandler;
import org.opensilk.music.ui.cards.handler.ArtistCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongGroupCardClickHandler;
import org.opensilk.music.ui.profile.adapter.GridAdapter;
import org.opensilk.music.ui.profile.loader.ArtistGridLoader;
import org.opensilk.common.dagger.qualifier.ForActivity;
import org.opensilk.common.dagger.qualifier.ForFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 7/9/14.
 */
public class ArtistFragment extends ListStickyParallaxHeaderFragment implements LoaderManager.LoaderCallbacks<List<Object>> {

    protected ArtworkImageView mHeroImage;
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mHeaderOverflow;

    private LocalArtist mArtist;

    @Inject
    GridAdapter mAdapter;

    public static ArtistFragment newInstance(Bundle args) {
        ArtistFragment f = new ArtistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mArtist = getArguments().getParcelable(Config.EXTRA_DATA);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // hero image
        mHeroImage = (ArtworkImageView) mHeroContainer.findViewById(R.id.hero_image);
        mHeroImage.setPaletteListener(this);
        // Load header images
        ArtworkManager.loadArtistImage(mArtist.name, mHeroImage);
        // Load header text
        mInfoTitle = ButterKnife.findById(mStickyHeader, R.id.info_title);
        mInfoTitle.setText(mArtist.name);
        mInfoSubTitle = ButterKnife.findById(mStickyHeader, R.id.info_subtitle);
        mInfoSubTitle.setVisibility(View.GONE);
        //overflow
        mHeaderOverflow = ButterKnife.findById(mStickyHeader, R.id.profile_header_overflow);
        final ArtistCard artistCard = new ArtistCard(getActivity(), mArtist);
        inject(artistCard);
        mHeaderOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                artistCard.onOverflowClicked(v);
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
    public Loader<List<Object>> onCreateLoader(int id, Bundle args) {
        return new ArtistGridLoader(getActivity(), mArtist);
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
