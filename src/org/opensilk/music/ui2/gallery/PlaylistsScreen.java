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

package org.opensilk.music.ui2.gallery;

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxCursorLoader;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(PlaylistsScreen.Module.class)
@WithGalleryPageViewPresenter(PlaylistsScreen.Presenter.class)
public class PlaylistsScreen extends Screen {

    @dagger.Module(
            addsTo = GalleryScreen.Module.class,
            injects = Presenter.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends BasePresenter<Playlist> {

        final Loader loader;

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor, Loader loader) {
            super(preferences, artworkRequestor);
            this.loader = loader;
        }

        @Override
        protected void load() {
//            subscription = loader.getListObservable().subscribe(new Action1<List<Playlist>>() {
//                @Override
//                public void call(List<Playlist> playlists) {
//                    addItems(playlists);
//                }
//            });
            items = new ArrayList<>();
            subscription = loader.getPlaylists().subscribe(new Action1<Playlist>() {
                @Override
                public void call(Playlist playlist) {
                    Timber.v("Adding playlist %s", playlist.mPlaylistName);
                    //addItem(playlist);
                    items.add(playlist);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    //TODO
                }
            });
        }

        @Override
        protected void handleItemClick(Context context, Playlist item) {
            NavUtils.openPlaylistProfile(context, item);
        }

        @Override
        protected BaseAdapter<Playlist> newAdapter(List<Playlist> items) {
            return new Adapter(items, artworkRequestor);
        }

        @Override
        protected boolean isStaggered() {
            return true;
        }

        @Override
        public ActionBarOwner.MenuConfig getMenuConfig() {
            return null;
        }

    }

    @Singleton
    public static class Loader {

        final Context context;

        @Inject
        public Loader(@ForApplication Context context) {
            this.context = context;
        }

        @DebugLog
        public Observable<Playlist> getPlaylists() {
            RxCursorLoader<LocalSong> lastAddedLoader = new RxCursorLoader<LocalSong>(context,
                    Uris.EXTERNAL_MEDIASTORE_MEDIA,
                    Projections.LOCAL_SONG,
                    Selections.LAST_ADDED,
                    SelectionArgs.LAST_ADDED(),
                    SortOrder.LAST_ADDED) {
                @Override
                protected LocalSong makeFromCursor(Cursor c) {
                    return CursorHelpers.makeLocalSongFromCursor(c);
                }
            };

            RxCursorLoader<Playlist> playlistLoader = new RxCursorLoader<Playlist>(context,
                    Uris.EXTERNAL_MEDIASTORE_PLAYLISTS,
                    Projections.PLAYLIST,
                    Selections.PLAYLIST,
                    SelectionArgs.PLAYLIST,
                    MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER) {
                @Override
                protected Playlist makeFromCursor(Cursor c) {
                    long id = c.getInt(c.getColumnIndexOrThrow(BaseColumns._ID));
                    String name = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME));
                    RxCursorLoader<LocalSong> songsLoader = new RxCursorLoader<LocalSong>(context,
                            Uris.PLAYLIST(id),
                            Projections.PLAYLIST_SONGS,
                            Selections.PLAYLIST_SONGS,
                            SelectionArgs.PLAYLIST_SONGS,
                            SortOrder.PLAYLIST_SONGS) {
                        @Override
                        protected LocalSong makeFromCursor(Cursor c) {
                            return CursorHelpers.makeLocalSongFromCursor(c);
                        }
                    };
                    Observable<Playlist> playlist = performSomeMagick(
                            songsLoader.createObservable(),
                            id, name);
                    return playlist.toBlocking().first();
                }
            };

            Observable<Playlist> lastAddedObservable = performSomeMagick(
                    // performSomeMagick subscribes us on io so we dont need to do that here
                    lastAddedLoader.createObservable(),
                    -1, context.getResources().getString(R.string.playlist_last_added));
            //subscribing on io so we dont block whatever thread we are created on
            Observable<Playlist> playlistObservable = playlistLoader.createObservable().subscribeOn(Schedulers.io());

            return Observable.mergeDelayError(lastAddedObservable, playlistObservable).observeOn(AndroidSchedulers.mainThread());
        }

        @DebugLog
        public Observable<Playlist> performSomeMagick(Observable<LocalSong> observable,
                                                      final long playlistId,
                                                      final String playlistName) {
            //songs
            Observable<List<Long>> songs = observable.subscribeOn(Schedulers.io()).map(new Func1<LocalSong, Long>() {
                @Override
                public Long call(LocalSong localSong) {
                    return localSong.songId;
                }
                // collect output into list
            }).collect(new ArrayList<Long>(), new Action2<List<Long>, Long>() {
                @Override
                public void call(List<Long> longs, Long aLong) {
                    longs.add(aLong);
                }
            });
            //albums
            Observable<List<Long>> albums = observable.subscribeOn(Schedulers.io()).map(new Func1<LocalSong, Long>() {
                @Override
                public Long call(LocalSong localSong) {
                    return localSong.albumId;
                }
                //only want unique albums, //collect output into list
            }).distinct().collect(new ArrayList<Long>(), new Action2<List<Long>, Long>() {
                @Override
                public void call(List<Long> longs, Long aLong) {
                    longs.add(aLong);
                }
            });
            // zip the songs and albums into a playlist
            return Observable.zip(songs, albums,
                    new Func2<List<Long>, List<Long>, Playlist>() {
                        @Override
                        public Playlist call(List<Long> songs, List<Long> albums) {
                            Collections.sort(albums);
                            return new Playlist(playlistId,
                                    playlistName,
                                    songs.size(),
                                    albums.size(),
                                    toArray(songs),
                                    toArray(albums));
                        }
                    });
        }

        // Modified Longs.toArray from Guava
        public static long[] toArray(Collection<Long> collection) {
            Object[] boxedArray = collection.toArray();
            int len = boxedArray.length;
            long[] array = new long[len];
            for (int i = 0; i < len; i++) {
                array[i] = ((Long) boxedArray[i]).longValue();
            }
            return array;
        }
    }

    static class Adapter extends BaseAdapter<Playlist> {

        Adapter(List<Playlist> items, ArtworkRequestManager artworkRequestor) {
            super(items, artworkRequestor);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Playlist playlist = getItem(position);
            holder.title.setText(playlist.mPlaylistName);
            holder.subtitle.setText(MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, playlist.mSongNumber));
            switch (holder.artNumber) {
                case 4:
                    if (playlist.mAlbumIds.length >= 4) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork4,
                                playlist.mAlbumIds[3], ArtworkType.THUMBNAIL));
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork3,
                                playlist.mAlbumIds[2], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 2:
                    if (playlist.mAlbumIds.length >= 2) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork2,
                                playlist.mAlbumIds[1], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 1:
                    if (playlist.mAlbumIds.length >= 1) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork,
                                playlist.mAlbumIds[0], ArtworkType.THUMBNAIL));
                    } else {
                        holder.artwork.setImageResource(R.drawable.default_artwork);
                    }
            }
        }

        @Override
        protected boolean quadArtwork(int position) {
            return getItem(position).mAlbumIds.length >= 4;
        }

        @Override
        protected boolean dualArtwork(int position) {
            return getItem(position).mAlbumIds.length >= 2;
        }
    }
}
