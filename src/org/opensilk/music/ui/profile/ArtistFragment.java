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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.utils.MusicUtils;
import com.manuelpeinado.fadingactionbar.extras.actionbarcompat.FadingActionBarHelper;

import org.opensilk.music.adapters.AlbumGridCardCursorAdapter;
import org.opensilk.music.adapters.ProfileAlbumListCardCursorAdapter;
import org.opensilk.music.loaders.ArtistAlbumCursorLoader;

import it.gmariotti.cardslib.library.view.CardGridView;
import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by drew on 2/21/14.
 */
public class ArtistFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /* Manages our views */
    protected FadingActionBarHelper mFadingHelper;
    /* main content */
    protected CardListView mListView;
    protected ProfileAlbumListCardCursorAdapter mAdapter;
    /* header image */
    protected ImageView mHeaderImage;
    /* header overlay stuff */
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mOverflowButton;

    private Artist mArtist;

    public static ArtistFragment newInstance(Bundle args) {
        ArtistFragment f = new ArtistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Init the helper here so its around for onCreateView
        mFadingHelper = new FadingActionBarHelper()
                .actionBarBackground(R.drawable.ab_solid_orpheus)
                .headerLayout(R.layout.profile_header_image)
                .headerOverlayLayout(R.layout.profile_header_overlay_artist)
                .contentLayout(R.layout.profile_card_listview);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            // We're fucked;
            getActivity().finish();
        }
        mArtist = getArguments().getParcelable(Config.EXTRA_DATA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = mFadingHelper.createView(inflater);
        mListView = (CardListView) v.findViewById(android.R.id.list);
        mHeaderImage = (ImageView) v.findViewById(R.id.artist_image_header);
        mInfoTitle = (TextView) v.findViewById(R.id.info_title);
        mInfoSubTitle = (TextView) v.findViewById(R.id.info_subtitle);
        mOverflowButton = v.findViewById(R.id.profile_header_overflow);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Load header images
        ImageFetcher.getInstance(getActivity()).loadArtistImage(mArtist.mArtistName, mHeaderImage);
        // Load header text
        mInfoTitle.setText(mArtist.mArtistName);
        mInfoSubTitle.setText(MusicUtils.makeLabel(getActivity(), R.plurals.Nalbums, mArtist.mAlbumNumber));
        // initialize header overflow
//        mOverflowButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                PopupMenu menu = new PopupMenu(v.getContext(), v);
//                menu.inflate(card.getOverflowMenuId());
//                menu.setOnMenuItemClickListener(card.getOverflowPopupMenuListener());
//                menu.show();
//            }
//        });
        // Init the fading action bar
        mFadingHelper.initActionBar(getActivity());
        // init the adapter
        mAdapter = new ProfileAlbumListCardCursorAdapter(getActivity());
        // set the adapter
        mListView.setAdapter(mAdapter);
        // start the loader
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mArtist.mArtistId);
        getLoaderManager().initLoader(0, b, this);
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ArtistAlbumCursorLoader(getActivity(), args.getLong(Config.ID));
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
