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
import android.widget.TextView;

import com.andrew.apollo.Config;
import org.opensilk.music.R;
import com.andrew.apollo.model.LocalAlbum;
import com.squareup.otto.Bus;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui.cards.AlbumCard;
import org.opensilk.music.ui.cards.handler.AlbumCardClickHandler;
import org.opensilk.music.ui.cards.handler.SongCardClickHandler;
import org.opensilk.music.ui.profile.adapter.SongCollectionAdapter;
import org.opensilk.music.ui.profile.loader.AlbumSongLoader;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import butterknife.ButterKnife;

/**
 * Created by drew on 7/10/14.
 */
public class AlbumFragment extends ListStickyParallaxHeaderFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    protected ArtworkImageView mHeroImage;
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mHeaderOverflow;

    private LocalAlbum mAlbum;

    protected SongCollectionAdapter mAdapter;
    @Inject @ForFragment
    protected Bus mBus;

    public static AlbumFragment newInstance(Bundle args) {
        AlbumFragment f = new AlbumFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlbum = getArguments().getParcelable(Config.EXTRA_DATA);
        mAdapter = new SongCollectionAdapter(getActivity(), this, true,
                Uris.LOCAL_ALBUM_SONGS,
                Projections.LOCAL_SONG,
                Selections.LOCAL_ALBUM_SONGS,
                SelectionArgs.LOCAL_ALBUM_SONGS(mAlbum.albumId),
                SortOrder.LOCAL_ALBUM_SONGS);
        // start the loader
        getLoaderManager().initLoader(0, null, this);
        registerHandlers();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // hero image
        mHeroImage = (ArtworkImageView) mHeroContainer.findViewById(R.id.hero_image);
        mHeroImage.setPaletteListener(this);
        // Load header images
        ArtworkManager.loadAlbumImage(mAlbum.artistName, mAlbum.name, CursorHelpers.generateArtworkUri(mAlbum.albumId), mHeroImage);
        // Load header text
        mInfoTitle = ButterKnife.findById(mStickyHeader, R.id.info_title);
        mInfoTitle.setText(mAlbum.name);
        mInfoSubTitle = ButterKnife.findById(mStickyHeader, R.id.info_subtitle);
        mInfoSubTitle.setText(mAlbum.artistName);
        //overflow
        mHeaderOverflow = ButterKnife.findById(mStickyHeader, R.id.profile_header_overflow);
        final AlbumCard albumCard = new AlbumCard(getActivity(), mAlbum);
        inject(albumCard);
        mHeaderOverflow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                albumCard.onOverflowClicked(v);
            }
        });
        // set list adapter
        mList.setAdapter(mAdapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterHandlers();
    }

    @Override
    protected Object[] getModules() {
        return new Object[] {
                new ProfileModule(),
        };
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

    private AlbumCardClickHandler mAlbumHandler;
    private SongCardClickHandler mSongHandler;

    private void registerHandlers() {
        mAlbumHandler = getObjectGraph().get(AlbumCardClickHandler.class);
        mSongHandler = getObjectGraph().get(SongCardClickHandler.class);
        mBus.register(mAlbumHandler);
        mBus.register(mSongHandler);
    }

    private void unregisterHandlers() {
        mBus.unregister(mAlbumHandler);
        mBus.unregister(mSongHandler);
    }
}
