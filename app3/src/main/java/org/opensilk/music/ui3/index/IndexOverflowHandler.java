/*
 * Copyright (c) 2015 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.music.ui3.index;

import android.content.Context;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortarfragment.FragmentManagerOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Folder;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Model;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;
import org.opensilk.music.model.TrackList;
import org.opensilk.music.model.spi.Bundleable;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.MenuAction;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.common.OverflowClickListener;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by drew on 5/9/15.
 */
@ScreenScope
public class IndexOverflowHandler implements OverflowClickListener {

    public static final int[] ALBUMS = new int[]{
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
//            R.menu.popup_add_to_playlist,
//            R.menu.popup_more_by_artist,
//            R.menu.popup_delete,
    };

    public static final int[] ARTISTS = new int[] {
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
//            R.menu.popup_add_to_playlist,
//            R.menu.popup_delete,
    };

    public static final int[] FOLDERS = new int[] {
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
//            R.menu.popup_delete
    };

    public static final int[] GENRES = new int[] {
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
//            R.menu.popup_add_to_playlist,
    };

    public static final int[] PLAYLISTS = new int[] {
            R.menu.popup_play_all,
            R.menu.popup_shuffle_all,
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
//            R.menu.popup_rename,
//            R.menu.popup_delete,
    };

    public static final int[] TRACKS = new int[] {
            R.menu.popup_play_next,
            R.menu.popup_add_to_queue,
//            R.menu.popup_add_to_playlist,
//            R.menu.popup_more_by_artist,
//            R.menu.popup_set_ringtone,
//            R.menu.popup_delete,
    };

    final PlaybackController playbackController;
    final AppPreferences appPreferences;
    final FragmentManagerOwner fm;
    final Uri loaderUri;

    @Inject
    public IndexOverflowHandler(
            PlaybackController playbackController,
            AppPreferences appPreferences,
            FragmentManagerOwner fm,
            @Named("loader_uri") Uri loaderUri
    ) {
        this.playbackController = playbackController;
        this.appPreferences = appPreferences;
        this.fm = fm;
        this.loaderUri = loaderUri;
    }

    public void onBuildMenu(Model item, MenuInflater inflater, Menu menu) {
        if (item instanceof Album) {

        } else if (item instanceof Artist) {

        } else if (item instanceof Genre) {

        } else if (item instanceof TrackList) {

        } else if (item instanceof Track) {

        } else if (item instanceof Folder) {

        } else if (item instanceof Playlist) {

        }
    }

    public boolean onMenuItemClick(Context context, MenuAction action, Model item) {
        if (item instanceof Album) {

        } else if (item instanceof Artist) {

        } else if (item instanceof Genre) {

        } else if (item instanceof TrackList) {

        } else if (item instanceof Track) {

        } else if (item instanceof Folder) {

        } else if (item instanceof Playlist) {

        }
        return false;
    }

    @Override
    public void onBuildMenu(Context context, PopupMenu m, Bundleable item) {

    }

    @Override
    public boolean onItemClicked(Context context, OverflowAction action, Bundleable item) {

        if (true) return false;

        Uri uri = null;
        String sortOrder = null;
        if (item instanceof Album) {
//            uri = LibraryUris.albumTracks(libraryConfig.authority,
//                    libraryInfo.libraryId, item.getIdentity());
//            sortOrder = appPreferences.getString(appPreferences.makePluginPrefKey(libraryConfig,
//                    AppPreferences.ALBUM_TRACK_SORT_ORDER), TrackSortOrder.PLAYORDER);
        } else if (item instanceof Artist) {
//            uri = LibraryUris.artistTracks(libraryConfig.authority,
//                    libraryInfo.libraryId, item.getIdentity());
//            sortOrder = appPreferences.getString(appPreferences.makePluginPrefKey(libraryConfig,
//                    AppPreferences.ARTIST_TRACK_SORT_ORDER), TrackSortOrder.TITLE);
        } else if (item instanceof Folder) {
//            uri = LibraryUris.folderTracks(libraryConfig.authority,
//                    libraryInfo.libraryId, item.getIdentity());
//            sortOrder = appPreferences.getString(appPreferences.makePluginPrefKey(libraryConfig,
//                    AppPreferences.FOLDER_SORT_ORDER), FolderTrackSortOrder.A_Z);
        } else if (item instanceof Genre) {
//            uri = LibraryUris.genreTracks(libraryConfig.authority,
//                    libraryInfo.libraryId, item.getIdentity());
//            sortOrder = appPreferences.getString(appPreferences.makePluginPrefKey(libraryConfig,
//                    AppPreferences.GENRE_TRACK_SORT_ORDER), TrackSortOrder.TITLE);
        } else if (item instanceof Playlist) {
//            uri = LibraryUris.playlistTracks(libraryConfig.authority,
//                    libraryInfo.libraryId, item.getIdentity());
//            sortOrder = TrackSortOrder.PLAYORDER;
        } else if (item instanceof Track) {
//            uri = LibraryUris.track(libraryConfig.authority,
//                    libraryInfo.libraryId, item.getIdentity());
//            sortOrder = null;
        } else {
            return false;
        }

        switch (action) {
            case PLAY_ALL:
                playbackController.playTracksFrom(uri, 0, sortOrder);
                return true;
            case SHUFFLE_ALL:
                playbackController.shuffleTracksFrom(uri);
                return true;
            case ADD_TO_QUEUE: {
                if (item instanceof Track) {
                    playbackController.addAllToQueue(Collections.singletonList(uri));
                } else {
                    playbackController.addTracksToQueueFrom(uri, sortOrder);
                }
                return true;
            }
            case PLAY_NEXT: {
                if (item instanceof Track) {
                    playbackController.enqueueAllNext(Collections.singletonList(uri));
                } else {
                    playbackController.enqueueTracksNextFrom(uri, sortOrder);
                }
                return true;
            }
            case ADD_TO_PLAYLIST:
                //TODO
                return true;
            case MORE_BY_ARTIST:
                //TODO
                return true;
            case DELETE: {
//                String auth = libraryConfig.authority;
//                String lib = libraryInfo.libraryId;
//                DeleteRequest request;
//                if (item instanceof Album) {
//                    request = DeleteRequest.forAlbum(auth, lib, (Album)item);
//                } else if (item instanceof Artist) {
//                    request = DeleteRequest.forArtist(auth, lib, (Artist)item);
//                } else if (item instanceof Folder) {
//                    request = DeleteRequest.forFolder(auth, lib, (Folder) item);
//                } else if (item instanceof Playlist) {
//                    request = DeleteRequest.forPlaylist(auth, lib, (Playlist) item);
//                } else if (item instanceof Track) {
//                    request = DeleteRequest.forTrack(auth, lib, (Track)item, loaderUri);
//                } else {
//                    return false;
//                }
//                DeleteScreenFragment f = DeleteScreenFragment.ni(context, request);
//                fm.addFragment(f, true);
                return true;
            }
            default:
                return false;
        }
    }
}
