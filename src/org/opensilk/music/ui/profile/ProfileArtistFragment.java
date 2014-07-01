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
import android.widget.PopupMenu;
import android.widget.TextView;

import com.andrew.apollo.Config;
import com.andrew.apollo.R;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.ApolloUtils;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;
import com.manuelpeinado.fadingactionbar.extras.actionbarcompat.FadingActionBarHelper;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.opensilk.music.ui.cards.ArtistCard;
import org.opensilk.music.ui.cards.event.AlbumCardClick;
import org.opensilk.music.ui.cards.event.ArtistCardClick;
import org.opensilk.music.ui.profile.adapter.ProfileArtistAdapter;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.dialogs.AddToPlaylistDialog;
import org.opensilk.music.ui.profile.loader.ProfileArtistLoader;
import org.opensilk.music.util.Command;
import org.opensilk.music.util.CommandRunner;
import org.opensilk.music.widgets.BottomCropArtworkImageView;
import org.opensilk.silkdagger.qualifier.ForFragment;

import javax.inject.Inject;

import it.gmariotti.cardslib.library.view.CardListView;

import static org.opensilk.music.util.ConfigHelper.isLargeLandscape;

/**
 * Created by drew on 2/21/14.
 */
public class ProfileArtistFragment extends ProfileFadingBaseFragment<LocalArtist> {

    @Inject @ForFragment
    Bus mBus;

    private FragmentBusMonitor mBusMonitor;

    /* header overlay stuff */
    protected TextView mInfoTitle;
    protected TextView mInfoSubTitle;
    protected View mOverflowButton;

    private LocalArtist mArtist;

    public static ProfileArtistFragment newInstance(Bundle args) {
        ProfileArtistFragment f = new ProfileArtistFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mArtist = mBundleData;
        mBusMonitor = new FragmentBusMonitor();
        mBus.register(mBusMonitor);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Init the helper here so its around for onCreateView
        mFadingHelper = new FadingActionBarHelper()
                .actionBarBackground(mActionBarBackground)
                .headerLayout(R.layout.profile_header_image)
                .headerOverlayLayout(R.layout.profile_header_overlay_artist)
                .contentLayout(R.layout.card_listview);
        View v = mFadingHelper.createView(inflater);
        mListView = (CardListView) v.findViewById(android.R.id.list);
        // set the adapter
        mListView.setAdapter(mAdapter);
        mHeaderImage = (BottomCropArtworkImageView) v.findViewById(R.id.artist_image_header);
        mInfoTitle = (TextView) v.findViewById(R.id.info_title);
        mInfoSubTitle = (TextView) v.findViewById(R.id.info_subtitle);
        mOverflowButton = v.findViewById(R.id.profile_header_overflow);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Load header images
        ArtworkManager.loadArtistImage(mArtist.name, mHeaderImage);
        // Load header text
        mInfoTitle.setText(mArtist.name);
        mInfoSubTitle.setText(MusicUtils.makeLabel(getActivity(), R.plurals.Nalbums, mArtist.albumCount));
        // initialize header overflow
        final ArtistCard artistCard = new ArtistCard(getActivity(), mArtist);
        inject(artistCard);
        mOverflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                artistCard.onOverflowClicked(v);
            }
        });
//        mOverflowButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                PopupMenu m = new PopupMenu(v.getContext(), v);
//                m.inflate(R.menu.popup_play_all);
//                m.inflate(R.menu.popup_shuffle_all);
//                m.inflate(R.menu.popup_add_to_queue);
//                m.inflate(R.menu.popup_add_to_playlist);
//                m.inflate(R.menu.popup_delete);
//                m.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
//                    @Override
//                    public boolean onMenuItemClick(MenuItem item) {
//                        Command command = null;
//                        switch (item.getItemId()) {
//                            case R.id.popup_play_all:
//                                command = new Command() {
//                                    @Override
//                                    public CharSequence execute() {
//                                        LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), mArtist.artistId);
//                                        MusicUtils.playAllSongs(getActivity(), list, 0, false);
//                                        return null;
//                                    }
//                                };
//                                break;
//                            case R.id.popup_shuffle_all:
//                                command = new Command() {
//                                    @Override
//                                    public CharSequence execute() {
//                                        LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), mArtist.artistId);
//                                        MusicUtils.playAllSongs(getActivity(), list, 0, true);
//                                        return null;
//                                    }
//                                };
//                                break;
//                            case R.id.popup_add_to_queue:
//                                command = new Command() {
//                                    @Override
//                                    public CharSequence execute() {
//                                        LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), mArtist.artistId);
//                                        MusicUtils.addSongsToQueueSilent(getActivity(), list);
//                                        return getResources().getQuantityString(R.plurals.NNNtrackstoqueue, list.length, list.length);
//                                    }
//                                };
//                                break;
//                            case R.id.popup_add_to_playlist:
//                                long[] plist = MusicUtils.getSongListForArtist(getActivity(), mArtist.artistId);
//                                AddToPlaylistDialog.newInstance(plist)
//                                        .show(getChildFragmentManager(), "AddToPlaylistDialog");
//                                return true;
//                            case R.id.popup_delete:
//                                long[] dlist = MusicUtils.getSongListForArtist(getActivity(), mArtist.artistId);
//                                DeleteDialog.newInstance(mArtist.name, dlist, null) //TODO
//                                        .show(getChildFragmentManager(), "DeleteDialog");
//                                getActivity().finish();
//                                return true;
//                        }
//                        if (command != null) {
//                            ApolloUtils.execute(false, new CommandRunner(getActivity(), command));
//                            return true;
//                        }
//                        return false;
//                    }
//                });
//                m.show();
//            }
//        });
        // set the actionbar title
        setTitle(mArtist.name);
        // Init the fading action bar
        if (isLargeLandscape(getResources())) {
            mFadingHelper.fadeActionBar(false);
        }
        mFadingHelper.initActionBar(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mInfoTitle = null;
        mInfoSubTitle = null;
        mOverflowButton = null;
    }

    @Override
    public void onDestroy() {
        mBus.unregister(mBusMonitor);
        super.onDestroy();
    }

    /*
     * Loader Callbacks
     */

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new ProfileArtistLoader(getActivity(), args.getLong(Config.ID));
    }

    /*
     * Implement abstract methods
     */

    @Override
    protected CursorAdapter createAdapter() {
        return new ProfileArtistAdapter(getActivity(), this);
    }

    @Override
    protected Bundle createLoaderArgs() {
        final Bundle b = new Bundle();
        b.putLong(Config.ID, mBundleData.artistId);
        return b;
    }

    class FragmentBusMonitor {
        @Subscribe
        public void onArtistCardClick(ArtistCardClick e) {
            if (!(e.artist instanceof LocalArtist)) {
                return;
            }
            final LocalArtist artist = (LocalArtist) e.artist;
            Command command = null;
            switch (e.event) {
                case OPEN:
                    NavUtils.openArtistProfile(getActivity(), artist);
                    return;
                case PLAY_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), artist.artistId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, false);
                            return null;
                        }
                    };
                    break;
                case SHUFFLE_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), artist.artistId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, true);
                            return null;
                        }
                    };
                    break;
                case ADD_TO_QUEUE:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForArtist(getActivity(), artist.artistId);
                            MusicUtils.addSongsToQueueSilent(getActivity(), list);
                            return getResources().getQuantityString(R.plurals.NNNtrackstoqueue, list.length, list.length);
                        }
                    };
                    break;
                case ADD_TO_PLAYLIST:
                    long[] plist = MusicUtils.getSongListForArtist(getActivity(), artist.artistId);
                    AddToPlaylistDialog.newInstance(plist)
                            .show(getChildFragmentManager(), "AddToPlaylistDialog");
                    return;
                case DELETE:
                    long[] dlist = MusicUtils.getSongListForArtist(getActivity(), artist.artistId);
                    DeleteDialog.newInstance(artist.name, dlist, null) //TODO
                            .show(getChildFragmentManager(), "DeleteDialog");
                    return;
                default:
                    return;
            }
            if (command != null) {
                ApolloUtils.execute(false, new CommandRunner(getActivity(), command));
            }
        }

        @Subscribe
        public void onAlbumCardClick(AlbumCardClick e) {
            if (!(e.album instanceof LocalAlbum)) {
                return;
            }
            final LocalAlbum album = (LocalAlbum) e.album;
            Command command = null;
            switch (e.event) {
                case OPEN:
                    NavUtils.openAlbumProfile(getActivity(), album);
                    return;
                case PLAY_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), album.albumId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, false);
                            return null;
                        }
                    };
                    break;
                case SHUFFLE_ALL:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), album.albumId);
                            MusicUtils.playAllSongs(getActivity(), list, 0, true);
                            return null;
                        }
                    };
                    break;
                case ADD_TO_QUEUE:
                    command = new Command() {
                        @Override
                        public CharSequence execute() {
                            LocalSong[] list = MusicUtils.getLocalSongListForAlbum(getActivity(), album.albumId);
                            MusicUtils.addSongsToQueueSilent(getActivity(), list);
                            return getResources().getQuantityString(R.plurals.NNNtrackstoqueue, list.length, list.length);
                        }
                    };
                    break;
                case ADD_TO_PLAYLIST:
                    long[] plist = MusicUtils.getSongListForAlbum(getActivity(), album.albumId);
                    AddToPlaylistDialog.newInstance(plist)
                            .show(getChildFragmentManager(), "AddToPlaylistDialog");
                    return;
                case MORE_BY_ARTIST:
                    NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), album.artistName));
                    return;
                case DELETE:
                    long[] dlist = MusicUtils.getSongListForAlbum(getActivity(), album.albumId);
                    DeleteDialog.newInstance(album.name, dlist, null) //TODO
                            .show(getChildFragmentManager(), "DeleteDialog");
                    return;
                default:
                    return;
            }
            if (command != null) {
                ApolloUtils.execute(false, new CommandRunner(getActivity(), command));
            }
        }
    }

}
