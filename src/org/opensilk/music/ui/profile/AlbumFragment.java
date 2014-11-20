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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSong;

import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.loader.LocalAlbumSongLoader;
import org.opensilk.music.ui2.profile.SongCollectionAdapter;
import org.opensilk.music.ui2.ProfileActivity;
import org.opensilk.music.ui2.common.OverflowAction;
import org.opensilk.music.ui2.common.OverflowHandlers;
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
 * Created by drew on 7/10/14.
 */
public class AlbumFragment extends ListStickyParallaxHeaderFragment {

    @dagger.Module (
            addsTo = ProfileActivity.Module.class,
            injects = AlbumFragment.class
    )
    public static class Module {
        final LocalAlbum album;

        public Module(LocalAlbum album) {
            this.album = album;
        }

        @Provides @Singleton @Named("album")
        public long provideAlbumId() {
            return album.albumId;
        }
    }

    @Inject OverflowHandlers.LocalSongs mAdapterOverflowHandler;
    @Inject OverflowHandlers.LocalAlbums mAlbumsOverflowHandler;
    @Inject ArtworkRequestManager mRequestor;
    @Inject LocalAlbumSongLoader mLoader;

    LocalAlbum mAlbum;

    Subscription mLoaderSubscription;
    SongCollectionAdapter mAdapter;

    public static AlbumFragment newInstance(Bundle args) {
        AlbumFragment f = new AlbumFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlbum = getArguments().getParcelable(Config.EXTRA_DATA);
        ((DaggerInjector) getActivity()).getObjectGraph().plus(new Module(mAlbum)).inject(this);

        mAdapter = new SongCollectionAdapter(getActivity(), true);
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

}
