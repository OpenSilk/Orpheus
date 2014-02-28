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
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Artist;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.manuelpeinado.fadingactionbar.FadingActionBarHelper;

import org.opensilk.music.adapters.ProfileAlbumListCardCursorAdapter;
import org.opensilk.music.loaders.ArtistAlbumCursorLoader;
import org.opensilk.music.ui.cards.CardArtistList;

import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by drew on 2/21/14.
 */
public class ProfileArtistFragment extends ProfileFadingBaseFragment<Artist> {

    /* header overlay stuff */
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mOverflowButton;

    private Artist mArtist;

    public static ProfileArtistFragment newInstance(Bundle args) {
        ProfileArtistFragment f = new ProfileArtistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mArtist = mBundleData;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Init the helper here so its around for onCreateView
        mFadingHelper = new FadingActionBarHelper()
                .actionBarBackground(mActionBarBackground)
                .headerLayout(R.layout.profile_header_image)
                .headerOverlayLayout(R.layout.profile_header_overlay_artist)
                .contentLayout(R.layout.profile_card_listview);
        View v = mFadingHelper.createView(inflater);
        mListView = (CardListView) v.findViewById(android.R.id.list);
        // set the adapter
        mListView.setAdapter(mAdapter);
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
        ApolloUtils.getImageFetcher(getActivity()).loadArtistImage(mArtist.mArtistName, mHeaderImage);
        // Load header text
        mInfoTitle.setText(mArtist.mArtistName);
        mInfoSubTitle.setText(MusicUtils.makeLabel(getActivity(), R.plurals.Nalbums, mArtist.mAlbumNumber));
        final CardArtistList artistCard = new CardArtistList(getActivity(), mArtist);
        // initialize header overflow
        mOverflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.inflate(artistCard.getOverflowMenuId());
                menu.setOnMenuItemClickListener(artistCard.getOverflowPopupMenuListener());
                menu.show();
            }
        });
        // Init the fading action bar
        mFadingHelper.initActionBar(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mInfoTitle = null;
        mInfoSubTitle = null;
        mOverflowButton = null;
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ArtistAlbumCursorLoader(getActivity(), args.getLong(Config.ID));
    }

    /*
     * Implement abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new ProfileAlbumListCardCursorAdapter(getActivity());
    }

    @Override
    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mBundleData.mArtistId);
        return b;
    }

}
