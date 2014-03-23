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
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.ApolloUtils;
import com.manuelpeinado.fadingactionbar.extras.actionbarcompat.FadingActionBarHelper;

import org.opensilk.music.adapters.ProfileAlbumCursorAdapter;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.loaders.AlbumSongCursorLoader;
import org.opensilk.music.ui.cards.CardAlbumList;
import org.opensilk.music.widgets.BottomCropArtworkImageView;
import org.opensilk.music.widgets.ThumbnailArtworkImageView;

import static org.opensilk.music.util.ConfigHelper.isLargeLandscape;

/**
 * Created by drew on 2/21/14.
 */
public class ProfileAlbumFragment extends ProfileFadingBaseFragment<Album> {


    /* header overlay stuff */
    protected ThumbnailArtworkImageView mHeaderThumb;
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mOverflowButton;

    private Album mAlbum;

    public static ProfileAlbumFragment newInstance(Bundle args) {
        ProfileAlbumFragment f = new ProfileAlbumFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAlbum = mBundleData;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Init the helper here so its around for onCreateView
        mFadingHelper = new FadingActionBarHelper()
                .actionBarBackground(mActionBarBackground)
                .headerLayout(R.layout.profile_header_image)
                .headerOverlayLayout(R.layout.profile_header_overlay)
                .contentLayout(R.layout.profile_listview);
        View v = mFadingHelper.createView(inflater);
        mListView = (ListView) v.findViewById(android.R.id.list);
        // set the adapter
        mListView.setAdapter(mAdapter);
        mHeaderImage = (BottomCropArtworkImageView) v.findViewById(R.id.artist_image_header);
        mHeaderThumb = (ThumbnailArtworkImageView) v.findViewById(R.id.album_image_header);
        mInfoTitle = (TextView) v.findViewById(R.id.info_title);
        mInfoSubTitle = (TextView) v.findViewById(R.id.info_subtitle);
        mOverflowButton = v.findViewById(R.id.profile_header_overflow);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Load header images
        ArtworkManager.loadArtistImage(mAlbum.mArtistName, mHeaderImage);
        ArtworkManager.loadAlbumImage(mAlbum.mArtistName, mAlbum.mAlbumName,
                mAlbum.mAlbumId, mHeaderThumb);
        // Load header text
        mInfoTitle.setText(mAlbum.mAlbumName);
        mInfoSubTitle.setText(mAlbum.mArtistName);
        // initialize header overflow
        final CardAlbumList card = new CardAlbumList(getActivity(), mAlbum);
        mOverflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu menu = new PopupMenu(v.getContext(), v);
                menu.inflate(card.getOverflowMenuId());
                menu.setOnMenuItemClickListener(card.getOverflowPopupMenuListener());
                menu.show();
            }
        });
        // set the actionbar title
        setTitle(mAlbum.mAlbumName);
        // Init the fading action bar
        if (isLargeLandscape(getResources())) {
            mFadingHelper.fadeActionBar(false);
        }
        mFadingHelper.initActionBar(getActivity());
    }

    @Override
    public void onDestroyView() {
        mListView.setDivider(null); //HACK!!!!!!!!!! Fuckin ArrayOutOfBounds bullshit comment this and youll see
        super.onDestroyView();
        mHeaderThumb = null;
        mInfoTitle = null;
        mInfoSubTitle = null;
        mOverflowButton = null;
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AlbumSongCursorLoader(getActivity(), args.getLong(Config.ID));
    }

    /*
     * Implement Abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new ProfileAlbumCursorAdapter(getActivity());
    }

    @Override
    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mBundleData.mAlbumId);
        return b;
    }

}
