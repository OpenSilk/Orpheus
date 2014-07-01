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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.manuelpeinado.fadingactionbar.extras.actionbarcompat.FadingActionBarHelper;

import org.opensilk.music.adapters.ProfileAlbumCursorAdapter;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.loaders.AlbumSongCursorLoader;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.music.util.ConfigHelper;
import org.opensilk.music.widgets.BottomCropArtworkImageView;
import org.opensilk.music.widgets.ThumbnailArtworkImageView;

/**
 * Created by drew on 2/21/14.
 */
public class ProfileAlbumFragment extends ProfileFadingBaseFragment<LocalAlbum> {


    /* header overlay stuff */
    protected ThumbnailArtworkImageView mHeaderThumb;
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mOverflowButton;

    private LocalAlbum mAlbum;

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
        ArtworkManager.loadArtistImage(mAlbum.artistName, mHeaderImage);
        ArtworkManager.loadAlbumImage(mAlbum.artistName, mAlbum.name,
                mAlbum.artworkUri, mHeaderThumb);
        // Load header text
        mInfoTitle.setText(mAlbum.name);
        mInfoSubTitle.setText(mAlbum.artistName);
        // initialize header overflow
        mOverflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu m = new PopupMenu(v.getContext(), v);
                m.inflate(R.menu.popup_play_all);
                m.inflate(R.menu.popup_shuffle_all);
                m.inflate(R.menu.popup_add_to_queue);
                m.inflate(R.menu.popup_add_to_playlist);
                m.inflate(R.menu.popup_more_by_artist);
                m.inflate(R.menu.popup_delete);
                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Command command = null;
                        switch (item.getItemId()) {
                            case R.id.popup_play_all:
                                command = new Command() {
                                    @Override
                                    public CharSequence execute() {
                                        LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), mAlbum.albumId);
                                        MusicUtils.playAllSongs(getActivity(), list, 0, false);
                                        return null;
                                    }
                                };
                                break;
                            case R.id.popup_shuffle_all:
                                command = new Command() {
                                    @Override
                                    public CharSequence execute() {
                                        LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), mAlbum.albumId);
                                        MusicUtils.playAllSongs(getActivity(), list, 0, true);
                                        return null;
                                    }
                                };
                                break;
                            case R.id.popup_add_to_queue:
                                command = new Command() {
                                    @Override
                                    public CharSequence execute() {
                                        LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), mAlbum.albumId);
                                        MusicUtils.addSongsToQueueSilent(getActivity(), list);
                                        return getResources().getQuantityString(R.plurals.NNNtrackstoqueue, list.length, list.length);
                                    }
                                };
                                break;
                            case R.id.popup_add_to_playlist:
                                long[] plist = MusicUtils.getSongListForAlbum(getActivity(), mAlbum.albumId);
                                AddToPlaylistDialog.newInstance(plist)
                                        .show(getChildFragmentManager(), "AddToPlaylistDialog");
                                return true;
                            case R.id.popup_more_by_artist:
                                NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), mAlbum.artistName));
                                return true;
                            case R.id.popup_delete:
                                long[] dlist = MusicUtils.getSongListForAlbum(getActivity(), mAlbum.albumId);
                                DeleteDialog.newInstance(mAlbum.name, dlist, null) //TODO
                                        .show(getChildFragmentManager(), "DeleteDialog");
                                getActivity().finish();
                                return true;
                        }
                        if (command != null) {
                            ApolloUtils.execute(false, new CommandRunner(getActivity(), command));
                            return true;
                        }
                        return false;
                    }
                });
                m.show();
            }
        });
        // set the actionbar title
        setTitle(mAlbum.name);
        // Init the fading action bar
        if (ConfigHelper.isLargeLandscape(getResources())) {
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
        b.putLong(Config.ID, mBundleData.albumId);
        return b;
    }

}
