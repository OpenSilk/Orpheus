/*
 * Copyright (c) 2014 OpenSilk Productions LLC
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

package org.opensilk.music.ui2.common;

import android.content.Context;
import android.widget.PopupMenu;

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;

import org.opensilk.music.R;
import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.Artist;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui2.main.MusicServiceConnection;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.silkdagger.qualifier.ForApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.functions.Func0;

/**
 * Created by drew on 10/24/14.
 */
public class OverflowHandlers {

    public static void populateMenu(PopupMenu m, Album a) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
    }

    public static void populateMenu(PopupMenu m, Artist a) {
        m.inflate(R.menu.popup_play_all);
        m.inflate(R.menu.popup_shuffle_all);
        m.inflate(R.menu.popup_add_to_queue);
    }

    public static void populateMenu(PopupMenu m, Song s) {
        m.inflate(R.menu.popup_play_next);
        m.inflate(R.menu.popup_add_to_queue);
    }

    @Singleton
    public static class LocalAlbums implements OverflowHandler<LocalAlbum> {
        final MusicServiceConnection connection;
        final Context context;

        @Inject
        public LocalAlbums(MusicServiceConnection connection, @ForApplication Context context) {
            this.connection = connection;
            this.context = context;
        }

        public void populateMenu(PopupMenu m, LocalAlbum album) {
            m.inflate(R.menu.popup_play_all);
            m.inflate(R.menu.popup_shuffle_all);
            m.inflate(R.menu.popup_add_to_queue);
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_more_by_artist);
            m.inflate(R.menu.popup_delete);
        }

        public boolean handleClick(OverflowAction action, final LocalAlbum album) {
            final long albumId = album.albumId;
            switch (action) {
                case PLAY_ALL:
                    connection.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getLocalSongListForAlbum(context, albumId);
                        }
                    }, 0, false);
                    return true;
                case SHUFFLE_ALL:
                    connection.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getLocalSongListForAlbum(context, albumId);
                        }
                    }, 0, true);
                    return true;
                case ADD_TO_QUEUE:
                    connection.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getLocalSongListForAlbum(context, albumId);
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
//                    long[] plist = CursorHelpers.getSongIdsForAlbum(context, album.albumId);
//                    AddToPlaylistDialog.newInstance(plist)
//                            .show(context.getSupportFragmentManager(), "AddToPlaylistDialog");
                    return true;
                case MORE_BY_ARTIST:
//                    NavUtils.openArtistProfile(context, MusicUtils.makeArtist(context, album.artistName));
                    return true;
                case DELETE:
//                    long[] dlist = CursorHelpers.getSongIdsForAlbum(getActivity(), album.albumId);
//                    DeleteDialog.newInstance(album.name, dlist, null) //TODO
//                            .show(getActivity().getSupportFragmentManager(), "DeleteDialog");
                    return true;
                default:
                    return false;
            }
        }
    }

    @Singleton
    public static class LocalArtists implements OverflowHandler<LocalArtist> {
        final MusicServiceConnection connection;
        final Context context;

        @Inject
        public LocalArtists(MusicServiceConnection connection, @ForApplication Context context) {
            this.connection = connection;
            this.context = context;
        }

        public void populateMenu(PopupMenu m, LocalArtist artist) {
            m.inflate(R.menu.popup_play_all);
            m.inflate(R.menu.popup_shuffle_all);
            m.inflate(R.menu.popup_add_to_queue);
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_delete);
        }

        public boolean handleClick(OverflowAction action, final LocalArtist artist) {
            final long artistId = artist.artistId;
            switch (action) {
                case PLAY_ALL:
                    connection.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForLocalArtist(context, artistId);
                        }
                    }, 0, false);
                    return true;
                case SHUFFLE_ALL:
                    connection.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForLocalArtist(context, artistId);
                        }
                    }, 0, true);
                    return true;
                case ADD_TO_QUEUE:
                    connection.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForLocalArtist(context, artistId);
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
//                    long[] plist = MusicUtils.getSongListForArtist(getActivity(), artist.artistId);
//                    AddToPlaylistDialog.newInstance(plist)
//                            .show(getActivity().getSupportFragmentManager(), "AddToPlaylistDialog");
                    return true;
                case DELETE:
//                    long[] dlist = MusicUtils.getSongListForArtist(getActivity(), artist.artistId);
//                    DeleteDialog.newInstance(artist.name, dlist, null) //TODO
//                            .show(getActivity().getSupportFragmentManager(), "DeleteDialog");
                    return true;
                default:
                    return false;
            }
        }
    }

    @Singleton
    public static class Genres implements OverflowHandler<Genre> {
        final MusicServiceConnection connection;
        final Context context;

        @Inject
        public Genres(MusicServiceConnection connection, @ForApplication Context context) {
            this.connection = connection;
            this.context = context;
        }

        public void populateMenu(PopupMenu m, Genre genre) {
            m.inflate(R.menu.popup_play_all);
            m.inflate(R.menu.popup_shuffle_all);
            m.inflate(R.menu.popup_add_to_queue);
            m.inflate(R.menu.popup_add_to_playlist);
        }

        public boolean handleClick(OverflowAction action, final Genre genre) {
            final long genreId = genre.mGenreId;
            switch (action) {
                case PLAY_ALL:
                    connection.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForGenre(context, genreId);
                        }
                    }, 0, false);
                    return true;
                case SHUFFLE_ALL:
                    connection.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForGenre(context, genreId);
                        }
                    }, 0, true);
                    return true;
                case ADD_TO_QUEUE:
                    connection.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForGenre(context, genreId);
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
//                    long[] plist = MusicUtils.getSongListForGenre(getActivity(), genre.mGenreId);
//                    AddToPlaylistDialog.newInstance(plist)
//                            .show(getActivity().getSupportFragmentManager(), "AddToPlaylistDialog");
                    return true;
                default:
                    return false;
            }
        }
    }

    @Singleton
    public static class Playlists implements OverflowHandler<Playlist> {
        final MusicServiceConnection connection;
        final Context context;

        @Inject
        public Playlists(MusicServiceConnection connection, @ForApplication Context context) {
            this.connection = connection;
            this.context = context;
        }

        public void populateMenu(PopupMenu m, Playlist playlist) {
            m.inflate(R.menu.popup_play_all);
            m.inflate(R.menu.popup_shuffle_all);
            m.inflate(R.menu.popup_add_to_queue);
            if (playlist.mPlaylistId != -2) {
                // cant rename or delete last added
                m.inflate(R.menu.popup_rename);
                m.inflate(R.menu.popup_delete);
            }
        }

        public boolean handleClick(OverflowAction action, final Playlist playlist) {
            final long playlistId = playlist.mPlaylistId;
            switch (action) {
                case PLAY_ALL:
                    connection.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            if (playlistId == -2) {
                                return CursorHelpers.getSongsForLastAdded(context);
                            } else {
                                return CursorHelpers.getSongsForPlaylist(context, playlistId);
                            }
                        }
                    }, 0, false);
                    return true;
                case SHUFFLE_ALL:
                    connection.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            if (playlistId == -2) {
                                return CursorHelpers.getSongsForLastAdded(context);
                            } else {
                                return CursorHelpers.getSongsForPlaylist(context, playlistId);
                            }
                        }
                    }, 0, true);
                    return true;
                case ADD_TO_QUEUE:
                    connection.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            if (playlistId == -2) {
                                return CursorHelpers.getSongsForLastAdded(context);
                            } else {
                                return CursorHelpers.getSongsForPlaylist(context, playlistId);
                            }
                        }
                    });
                    return true;
                case RENAME:
//                    RenamePlaylist.getInstance(playlist.mPlaylistId)
//                            .show(getActivity().getSupportFragmentManager(), "RenameDialog");
                    return true;
                case DELETE:
//                    new AlertDialog.Builder(getActivity())
//                            .setTitle(getActivity().getString(R.string.delete_dialog_title, playlist.mPlaylistName))
//                            .setPositiveButton(R.string.context_menu_delete, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(final DialogInterface dialog, final int which) {
//                                    final Uri mUri = ContentUris.withAppendedId(
//                                            MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
//                                            playlist.mPlaylistId);
//                                    getActivity().getContentResolver().delete(mUri, null, null);
//                                    getActivity().getContentResolver().notifyChange(MusicProvider.PLAYLIST_URI, null);
//                                    MusicUtils.refresh();
//                                }
//                            })
//                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(final DialogInterface dialog, final int which) {
//                                    dialog.dismiss();
//                                }
//                            })
//                            .setMessage(R.string.cannot_be_undone)
//                            .create()
//                            .show();
                    return true;
                default:
                    return false;
            }
        }
    }

    @Singleton
    public static class LocalSongs implements OverflowHandler<LocalSong> {
        final MusicServiceConnection connection;
        final Context context;

        @Inject
        public LocalSongs(MusicServiceConnection connection, @ForApplication Context context) {
            this.connection = connection;
            this.context = context;
        }

        public void populateMenu(PopupMenu m, LocalSong song) {
            m.inflate(R.menu.popup_play_next);
            m.inflate(R.menu.popup_add_to_queue);
            m.inflate(R.menu.popup_add_to_playlist);
            m.inflate(R.menu.popup_more_by_artist);
            m.inflate(R.menu.popup_set_ringtone);
            m.inflate(R.menu.popup_delete);
        }

        public void play(final LocalSong song) {
            connection.playAllSongs(new Func0<Song[]>() {
                @Override
                public Song[] call() {
                    return new Song[]{song};
                }
            }, 0, false);
        }

        public boolean handleClick(OverflowAction action, final LocalSong song) {
            switch (action) {
                case PLAY_NEXT:
                    connection.enqueueNext(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return new Song[]{song};
                        }
                    });
                    return true;
                case ADD_TO_QUEUE:
                    connection.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return new Song[]{song};
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
//                    if (song instanceof LocalSong) {
//                        LocalSong localsong = (LocalSong) song;
//                        AddToPlaylistDialog.newInstance(new long[]{localsong.songId})
//                                .show(getActivity().getSupportFragmentManager(), "AddToPlaylistDialog");
//                    }
                    return true;
                case MORE_BY_ARTIST:
//                    if (song instanceof LocalSong) {
//                        LocalSong localsong = (LocalSong) song;
//                        NavUtils.openArtistProfile(getActivity(), MusicUtils.makeArtist(getActivity(), localsong.artistName));
//                    }
                    return true;
                case SET_RINGTONE:
//                    if (song instanceof LocalSong) {
//                        LocalSong localsong = (LocalSong) song;
//                        MusicUtils.setRingtone(getActivity(), localsong.songId);
//                    }
                    return true;
                case DELETE:
//                    if (song instanceof LocalSong) {
//                        LocalSong localsong = (LocalSong) song;
//                        DeleteDialog.newInstance(localsong.name, new long[]{localsong.songId}, null)
//                                .show(getActivity().getSupportFragmentManager(), "DeleteDialog");
//                    }
                    return true;
                default:
                    return false;
            }
        }
    }

}
