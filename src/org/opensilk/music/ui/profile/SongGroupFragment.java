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
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.LocalSongGroup;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui.profile.adapter.SongCollectionAdapter;
import org.opensilk.music.ui.profile.loader.SongGroupLoader;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
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
public class SongGroupFragment extends ListStickyParallaxHeaderFragment {

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = SongGroupFragment.class
    )
    public static class Module {
        final LocalSongGroup songGroup;

        public Module(LocalSongGroup songGroup) {
            this.songGroup = songGroup;
        }

        @Provides @Singleton @Named("songgroup")
        public long[] provideSongIds() {
            return songGroup.songIds;
        }
    }

    @Inject OverflowHandlers.LocalSongs mAdapterOverflowHandler;
    @Inject OverflowHandlers.LocalSongGroups mSongGroupOverflowHandler;
    @Inject ArtworkRequestManager mRequestor;
    @Inject SongGroupLoader mLoader;

    int numHeros;
    LocalSongGroup mSongGroup;

    Subscription mLoaderSubscription;
    SongCollectionAdapter mAdapter;

    public static SongGroupFragment newInstance(Bundle args) {
        SongGroupFragment f = new SongGroupFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSongGroup = getArguments().getParcelable(Config.EXTRA_DATA);
        ((DaggerInjector) getActivity()).getObjectGraph().plus(new Module(mSongGroup)).inject(this);

        mAdapter = new SongCollectionAdapter(getActivity(), mAdapterOverflowHandler, mRequestor, false);
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
        // hero image
        AnimatedImageView heroImage = ButterKnife.findById(mHeroContainer, R.id.hero_image);
        if (mSongGroup.albumIds.length == 0) {
            if (heroImage != null) (heroImage).setDefaultImage();
        } else {
            if (mSongGroup.albumIds.length >= 1 && heroImage != null) {
                mRequestor.newAlbumRequest(heroImage, null, mSongGroup.albumIds[0], ArtworkType.LARGE);
            }
            AnimatedImageView heroImage2 = ButterKnife.findById(mHeroContainer, R.id.hero_image2);
            if (mSongGroup.albumIds.length >= 2 && heroImage2 != null) {
                mRequestor.newAlbumRequest(heroImage2, null, mSongGroup.albumIds[1], ArtworkType.LARGE);
            }
            AnimatedImageView heroImage3 = ButterKnife.findById(mHeroContainer, R.id.hero_image3);
            if (mSongGroup.albumIds.length >= 3 && heroImage3 != null) {
                mRequestor.newAlbumRequest(heroImage3, null, mSongGroup.albumIds[2], ArtworkType.LARGE);
            }
            AnimatedImageView heroImage4 = ButterKnife.findById(mHeroContainer, R.id.hero_image4);
            if (mSongGroup.albumIds.length >= 4 && heroImage4 != null) {
                mRequestor.newAlbumRequest(heroImage4, null, mSongGroup.albumIds[3], ArtworkType.LARGE);
            }
        }
        // Load header text
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_title).setText(mSongGroup.parentName);
        ButterKnife.<TextView>findById(mStickyHeader, R.id.info_subtitle).setText(mSongGroup.name);
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        for (int ii : OverflowHandlers.LocalSongGroups.MENUS) {
            inflater.inflate(ii, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            return mSongGroupOverflowHandler.handleClick(OverflowAction.valueOf(item.getItemId()), mSongGroup);
        } catch (IllegalArgumentException e) {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected int getListLayout() {
        return R.layout.profile_list_frame;
    }

    @Override
    protected int getHeaderLayout() {
        if (mSongGroup.albumIds.length >= 4) {
            numHeros = 4;
            return R.layout.profile_hero_quad_header;
        } else if (mSongGroup.albumIds.length >= 2) {
            numHeros = 2;
            return R.layout.profile_hero_dual_header;
        } else {
            numHeros = 0;
            return super.getHeaderLayout();
        }
    }

}
