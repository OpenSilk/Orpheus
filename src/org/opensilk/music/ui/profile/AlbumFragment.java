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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.cache.ImageFetcher;
import com.andrew.apollo.loaders.AlbumSongLoader;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.manuelpeinado.fadingactionbar.extras.actionbarcompat.FadingActionBarHelper;
import com.mobeta.android.dslv.DragSortListView;

import org.opensilk.music.adapters.ProfileAlbumCursorAdapter;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.loaders.AlbumSongCursorLoader;

/**
 * Created by drew on 2/21/14.
 */
public class AlbumFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AdapterView.OnItemClickListener {

    /* Manages our views */
    protected FadingActionBarHelper mFadingHelper;
    /* main content */
    protected DragSortListView mListView;
    protected ProfileAlbumCursorAdapter mAdapter;
    /* header image */
    protected ImageView mHeaderImage;
    /* header overlay stuff */
    protected ImageView mHeaderThumb;
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mOverflowButton;

    private Album mAlbum;

    public static AlbumFragment newInstance(Bundle args) {
        AlbumFragment f = new AlbumFragment();
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
                .headerOverlayLayout(R.layout.profile_header_overlay)
                .contentLayout(R.layout.profile_listview);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() == null) {
            // We're fucked;
            getActivity().finish();
        }
        mAlbum = getArguments().getParcelable(Config.EXTRA_DATA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = mFadingHelper.createView(inflater);
        mListView = (DragSortListView) v.findViewById(android.R.id.list);
        mHeaderImage = (ImageView) v.findViewById(R.id.artist_image_header);
        mHeaderThumb = (ImageView) v.findViewById(R.id.album_image_header);
        mInfoTitle = (TextView) v.findViewById(R.id.info_title);
        mInfoSubTitle = (TextView) v.findViewById(R.id.info_subtitle);
        mOverflowButton = v.findViewById(R.id.profile_header_overflow);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Load header images
        ImageFetcher.getInstance(getActivity()).loadArtistImage(mAlbum.mArtistName, mHeaderImage);
        ImageFetcher.getInstance(getActivity()).loadAlbumImage(
                mAlbum.mArtistName, mAlbum.mAlbumName, mAlbum.mAlbumId, mHeaderThumb);
        // Load header text
        mInfoTitle.setText(mAlbum.mAlbumName);
        mInfoSubTitle.setText(mAlbum.mArtistName);
        // initialize header overflow
        mOverflowButton.setOnClickListener(mOverflowListener);
        // Init the fading action bar
        mFadingHelper.initActionBar(getActivity());
        // init the adapter
        mAdapter = new ProfileAlbumCursorAdapter(getActivity());
        // set the adapter
        mListView.setAdapter(mAdapter);
        // start the loader
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mAlbum.mAlbumId);
        getLoaderManager().initLoader(0, b, this);
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new AlbumSongCursorLoader(getActivity(), args.getLong(Config.ID));
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    /*
     * On click callback
     */

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            return;
        }
        Cursor cursor = AlbumSongLoader.makeAlbumSongCursor(getActivity(), getArguments()
                .getLong(Config.ID));
        final long[] list = MusicUtils.getSongListForCursor(cursor);
        MusicUtils.playAll(getActivity(), list, position - 1, false);
        cursor.close();
    }

    protected final View.OnClickListener mOverflowListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            PopupMenu menu = new PopupMenu(v.getContext(), v);
            //Todo combine this and the CardAlbum* menus
            menu.inflate(R.menu.card_album);
            menu.setOnMenuItemClickListener(
                    new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.card_menu_play:
                                    MusicUtils.playAll(getActivity(), MusicUtils.getSongListForAlbum(getActivity(), mAlbum.mAlbumId), 0, false);
                                    return true;
                                case R.id.card_menu_add_queue:
                                    MusicUtils.addToQueue(getActivity(), MusicUtils.getSongListForAlbum(getActivity(), mAlbum.mAlbumId));
                                    return true;
                                case R.id.card_menu_add_playlist:
                                    AddToPlaylistDialog.newInstance(MusicUtils.getSongListForAlbum(getActivity(), mAlbum.mAlbumId))
                                            .show(((FragmentActivity) getActivity()).getSupportFragmentManager(), "AddToPlaylistDialog");
                                    return true;
                                case R.id.card_menu_more_by:
                                    NavUtils.openArtistProfile(getActivity(), mAlbum.mArtistName);
                                    return true;
                                case R.id.card_menu_delete:
                                    final String album = mAlbum.mAlbumName;
                                    DeleteDialog.newInstance(album, MusicUtils.getSongListForAlbum(getActivity(), mAlbum.mAlbumId),
                                            ImageFetcher.generateAlbumCacheKey(album, mAlbum.mArtistName))
                                            .show(((FragmentActivity) getActivity()).getSupportFragmentManager(), "DeleteDialog");
                                    return true;
                            }
                            return false;
                        }
                    });
            menu.show();
        }
    };

}
