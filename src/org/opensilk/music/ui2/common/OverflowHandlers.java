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

import com.andrew.apollo.menu.AddToPlaylistDialog;
import com.andrew.apollo.menu.DeleteDialog;
import com.andrew.apollo.menu.DeletePlaylistDialog;
import com.andrew.apollo.menu.RenamePlaylist;
import com.andrew.apollo.model.Genre;
import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.LocalSongGroup;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.model.RecentSong;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.music.R;
import org.opensilk.music.api.model.Song;
import org.opensilk.music.ui2.event.MakeToast;
import org.opensilk.music.MusicServiceConnection;
import org.opensilk.music.ui2.event.OpenDialog;
import org.opensilk.music.ui2.event.StartActivityForResult;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.common.dagger.qualifier.ForApplication;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import rx.functions.Func0;

/**
 * Created by drew on 10/24/14.
 */
public class OverflowHandlers {

    @Singleton
    public static class LocalAlbums implements OverflowHandler<LocalAlbum> {

        public static final int[] MENUS = new int[] {
                R.menu.popup_play_all,
                R.menu.popup_shuffle_all,
                R.menu.popup_add_to_queue,
                R.menu.popup_add_to_playlist,
                R.menu.popup_more_by_artist,
                R.menu.popup_delete,
        };

        final MusicServiceConnection musicService;
        final Context context;
        final EventBus bus;

        @Inject
        public LocalAlbums(MusicServiceConnection musicService,
                            @ForApplication Context context,
                            @Named("activity") EventBus bus) {
            this.musicService = musicService;
            this.context = context;
            this.bus = bus;
        }

        public void populateMenu(PopupMenu m, LocalAlbum album) {
            for (int ii : MENUS) {
                m.inflate(ii);
            }
        }

        public boolean handleClick(OverflowAction action, final LocalAlbum album) {
            final long albumId = album.albumId;
            switch (action) {
                case PLAY_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getLocalSongListForAlbum(context, albumId);
                        }
                    }, 0, false);
                    return true;
                case SHUFFLE_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getLocalSongListForAlbum(context, albumId);
                        }
                    }, 0, true);
                    return true;
                case ADD_TO_QUEUE:
                    musicService.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getLocalSongListForAlbum(context, albumId);
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
                    long[] plist = CursorHelpers.getSongIdsForAlbum(context, albumId);
                    bus.post(new OpenDialog(AddToPlaylistDialog.newInstance(plist)));
                    return true;
                case MORE_BY_ARTIST:
                    bus.post(new StartActivityForResult(NavUtils.makeArtistProfileIntent(context,
                            MusicUtils.makeArtist(context, album.artistName)), 0));
                    return true;
                case DELETE:
                    long[] dlist = CursorHelpers.getSongIdsForAlbum(context, albumId);
                    bus.post(new OpenDialog(DeleteDialog.newInstance(album.name, dlist)));
                    return true;
                default:
                    return false;
            }
        }
    }

    @Singleton
    public static class LocalArtists implements OverflowHandler<LocalArtist> {

        public static final int[] MENUS = new int[] {
                R.menu.popup_play_all,
                R.menu.popup_shuffle_all,
                R.menu.popup_add_to_queue,
                R.menu.popup_add_to_playlist,
                R.menu.popup_delete,
        };

        final MusicServiceConnection musicService;
        final Context context;
        final EventBus bus;

        @Inject
        public LocalArtists(MusicServiceConnection musicService,
                            @ForApplication Context context,
                            @Named("activity") EventBus bus) {
            this.musicService = musicService;
            this.context = context;
            this.bus = bus;
        }

        public void populateMenu(PopupMenu m, LocalArtist artist) {
            for (int ii : MENUS) {
                m.inflate(ii);
            }
        }

        public boolean handleClick(OverflowAction action, final LocalArtist artist) {
            final long artistId = artist.artistId;
            switch (action) {
                case PLAY_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForLocalArtist(context, artistId);
                        }
                    }, 0, false);
                    return true;
                case SHUFFLE_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForLocalArtist(context, artistId);
                        }
                    }, 0, true);
                    return true;
                case ADD_TO_QUEUE:
                    musicService.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForLocalArtist(context, artistId);
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
                    long[] plist = CursorHelpers.getSongIdsForArtist(context, artistId);
                    bus.post(new OpenDialog(AddToPlaylistDialog.newInstance(plist)));
                    return true;
                case DELETE:
                    long[] dlist = CursorHelpers.getSongIdsForArtist(context, artistId);
                    bus.post(new OpenDialog(DeleteDialog.newInstance(artist.name, dlist)));
                    return true;
                default:
                    return false;
            }
        }
    }

    @Singleton
    public static class Genres implements OverflowHandler<Genre> {

        public static final int[] MENUS = new int[] {
                R.menu.popup_play_all,
                R.menu.popup_shuffle_all,
                R.menu.popup_add_to_queue,
                R.menu.popup_add_to_playlist,
        };

        final MusicServiceConnection musicService;
        final Context context;
        final EventBus bus;

        @Inject
        public Genres(MusicServiceConnection musicService,
                            @ForApplication Context context,
                            @Named("activity") EventBus bus) {
            this.musicService = musicService;
            this.context = context;
            this.bus = bus;
        }

        public void populateMenu(PopupMenu m, Genre genre) {
            for (int ii : MENUS) {
                m.inflate(ii);
            }
        }

        public boolean handleClick(OverflowAction action, final Genre genre) {
            final long genreId = genre.mGenreId;
            switch (action) {
                case PLAY_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForGenre(context, genreId);
                        }
                    }, 0, false);
                    return true;
                case SHUFFLE_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForGenre(context, genreId);
                        }
                    }, 0, true);
                    return true;
                case ADD_TO_QUEUE:
                    musicService.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsForGenre(context, genreId);
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
                    long[] plist = CursorHelpers.getSongIdsForGenre(context, genreId);
                    bus.post(new OpenDialog(AddToPlaylistDialog.newInstance(plist)));
                    return true;
                default:
                    return false;
            }
        }
    }

    @Singleton
    public static class Playlists implements OverflowHandler<Playlist> {

        public static final int[] MENUS_COMMON = new int[] {
                R.menu.popup_play_all,
                R.menu.popup_shuffle_all,
                R.menu.popup_add_to_queue,
        };

        public static final int[] MENUS_USER = new int[] {
                R.menu.popup_rename,
                R.menu.popup_delete,
        };

        final MusicServiceConnection musicService;
        final Context context;
        final EventBus bus;

        @Inject
        public Playlists(MusicServiceConnection musicService,
                            @ForApplication Context context,
                            @Named("activity") EventBus bus) {
            this.musicService = musicService;
            this.context = context;
            this.bus = bus;
        }

        public void populateMenu(PopupMenu m, Playlist playlist) {
            for (int ii : MENUS_COMMON) {
                m.inflate(ii);
            }
            if (playlist.mPlaylistId != -2) {
                // cant rename or delete last added
                for (int ii : MENUS_USER) {
                    m.inflate(ii);
                }
            }
        }

        public boolean handleClick(OverflowAction action, final Playlist playlist) {
            final long playlistId = playlist.mPlaylistId;
            switch (action) {
                case PLAY_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
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
                    musicService.playAllSongs(new Func0<Song[]>() {
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
                    musicService.enqueueEnd(new Func0<Song[]>() {
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
                    if (playlistId != -2) {
                        bus.post(new OpenDialog(RenamePlaylist.getInstance(playlistId)));
                    }
                    return true;
                case DELETE:
                    if (playlistId != -2) {
                        bus.post(new OpenDialog(DeletePlaylistDialog.newInstance(playlist.mPlaylistName, playlistId)));
                    }
                    return true;
                default:
                    return false;
            }
        }
    }

    @Singleton
    public static class LocalSongs implements OverflowHandler<LocalSong> {

        public static final int[] MENUS = new int[] {
                R.menu.popup_play_next,
                R.menu.popup_add_to_queue,
                R.menu.popup_add_to_playlist,
                R.menu.popup_more_by_artist,
                R.menu.popup_set_ringtone,
                R.menu.popup_delete,
        };

        final MusicServiceConnection musicService;
        final Context context;
        final EventBus bus;

        @Inject
        public LocalSongs(MusicServiceConnection musicService,
                            @ForApplication Context context,
                            @Named("activity") EventBus bus) {
            this.musicService = musicService;
            this.context = context;
            this.bus = bus;
        }

        public void populateMenu(PopupMenu m, LocalSong song) {
            for (int ii : MENUS) {
                m.inflate(ii);
            }
        }

        public void play(final LocalSong song) {
            musicService.playAllSongs(new Func0<Song[]>() {
                @Override
                public Song[] call() {
                    return new Song[]{song};
                }
            }, 0, false);
        }

        public boolean handleClick(OverflowAction action, final LocalSong song) {
            switch (action) {
                case PLAY_NEXT:
                    musicService.enqueueNext(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return new Song[]{song};
                        }
                    });
                    return true;
                case ADD_TO_QUEUE:
                    musicService.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return new Song[]{song};
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
                    bus.post(new OpenDialog(AddToPlaylistDialog.newInstance(new long[]{song.songId})));
                    return true;
                case MORE_BY_ARTIST:
                    bus.post(new StartActivityForResult(NavUtils.makeArtistProfileIntent(context,
                            MusicUtils.makeArtist(context, song.artistName)), 0));
                    return true;
                case SET_RINGTONE:
                    MakeToast mt = MusicUtils.setRingtone(context, song.songId);
                    if (mt != null) bus.post(mt);
                    return true;
                case DELETE:
                    bus.post(new OpenDialog(DeleteDialog.newInstance(song.name, new long[]{song.songId})));
                    return true;
                default:
                    return false;
            }
        }
    }

    public static class LocalSongGroups implements OverflowHandler<LocalSongGroup> {

        public static final int[] MENUS = new int[] {
                R.menu.popup_play_all,
                R.menu.popup_shuffle_all,
                R.menu.popup_add_to_queue,
                R.menu.popup_add_to_playlist,
        };

        final MusicServiceConnection musicService;
        final Context context;
        final EventBus bus;

        @Inject
        public LocalSongGroups(MusicServiceConnection musicService,
                          @ForApplication Context context,
                          @Named("activity") EventBus bus) {
            this.musicService = musicService;
            this.context = context;
            this.bus = bus;
        }

        public void populateMenu(PopupMenu m, LocalSongGroup group) {
            for (int ii : MENUS) {
                m.inflate(ii);
            }
        }

        public boolean handleClick(OverflowAction action, final LocalSongGroup group) {
            switch (action) {
                case PLAY_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsFromId(context, group.songIds);
                        }
                    }, 0, false);
                    return true;
                case SHUFFLE_ALL:
                    musicService.playAllSongs(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsFromId(context, group.songIds);
                        }
                    }, 0, true);
                    return true;
                case ADD_TO_QUEUE:
                    musicService.enqueueEnd(new Func0<Song[]>() {
                        @Override
                        public Song[] call() {
                            return CursorHelpers.getSongsFromId(context, group.songIds);
                        }
                    });
                    return true;
                case ADD_TO_PLAYLIST:
                    bus.post(new OpenDialog(AddToPlaylistDialog.newInstance(group.songIds)));
                    return true;
                default:
                    return false;
            }
        }
    }

    public static class RecentSongs implements OverflowHandler<RecentSong> {

        public static final int[] MENUS_COMMON = new int[] {
                R.menu.popup_play_next,
        };

        public static final int[] MENUS_LOCAL = new int[] {
                R.menu.popup_add_to_playlist,
                R.menu.popup_more_by_artist,
                R.menu.popup_set_ringtone,
                R.menu.popup_delete,
        };

        final MusicServiceConnection musicService;
        final Context context;
        final EventBus bus;

        @Inject
        public RecentSongs(MusicServiceConnection musicService,
                               @ForApplication Context context,
                               @Named("activity") EventBus bus) {
            this.musicService = musicService;
            this.context = context;
            this.bus = bus;
        }

        public void populateMenu(PopupMenu m, RecentSong song) {
            for (int ii: MENUS_COMMON) {
                m.inflate(ii);
            }
            if (song.isLocal) {
                for (int ii: MENUS_LOCAL) {
                    m.inflate(ii);
                }
            }
        }

        public boolean handleClick(OverflowAction action, RecentSong song) {
            switch (action) {
                case PLAY_NEXT:
                    musicService.removeTrack(song.recentId);
                    musicService.enqueueNext(new long[]{song.recentId});
                    return true;
                case ADD_TO_PLAYLIST:
                    if (song.isLocal) {
                        try {
                            long id = Long.decode(song.identity);
                            bus.post(new OpenDialog(AddToPlaylistDialog.newInstance(new long[]{id})));
                        } catch (NumberFormatException ex) {
                            bus.post(new MakeToast(R.string.err_generic));
                        }
                    } // else unsupported
                    return true;
                case MORE_BY_ARTIST:
                    if (song.isLocal) {
                        bus.post(new StartActivityForResult(NavUtils.makeArtistProfileIntent(context,
                                MusicUtils.makeArtist(context, song.artistName)), 0));
                    } // else TODO
                    return true;
                case SET_RINGTONE:
                    if (song.isLocal) {
                        //TODO push to background
                        try {
                            long id = Long.decode(song.identity);
                            MakeToast toast = MusicUtils.setRingtone(context, id);
                            if (toast != null) bus.post(toast);
                        } catch (NumberFormatException ex) {
                            bus.post(new MakeToast(R.string.err_generic));
                        }
                    } // else unsupported
                    return true;
                case DELETE:
                    if (song.isLocal) {
                        try {
                            long id = Long.decode(song.identity);
                            bus.post(new OpenDialog(DeleteDialog.newInstance(song.name, new long[]{id})));
                        } catch (NumberFormatException ex) {
                            bus.post(new MakeToast(R.string.err_generic));
                        }
                    } // else unsupported
                    return true;
                default:
                    return false;
            }
        }

    }

}
