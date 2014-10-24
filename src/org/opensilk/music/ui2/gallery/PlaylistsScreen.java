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
import android.net.Uri;
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
import org.opensilk.music.ui2.loader.AbsGenrePlaylistLoader;
import org.opensilk.music.ui2.loader.RxCursorLoader;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import mortar.ViewPresenter;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(PlaylistsScreen.Module.class)
public class PlaylistsScreen extends Screen {

    @dagger.Module(
            addsTo = GalleryScreen.Module.class,
            injects = GalleryPageView.class
    )
    public static class Module {

        @Provides @Singleton
        public ViewPresenter<GalleryPageView> provideGalleryPagePresenter(Presenter presenter) {
            return presenter;
        }

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
            subscription = loader.getCollection().subscribe(new Action1<Playlist>() {
                @Override
                public void call(Playlist playlist) {
                    addItem(playlist);
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
    public static class Loader extends AbsGenrePlaylistLoader<Playlist> {

        @Inject
        public Loader(@ForApplication Context context) {
            super(context);
            setUri(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS);
            setProjection(Projections.PLAYLIST);
            setSelection(Selections.PLAYLIST);
            setSelectionArgs(SelectionArgs.PLAYLIST);
            setSortOrder(MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);

            setProjection2(Projections.PLAYLIST_SONGS);
            setSelection2(Selections.PLAYLIST_SONGS);
            setSelectionArgs2(SelectionArgs.PLAYLIST_SONGS);
            setSortOrder2(SortOrder.PLAYLIST_SONGS);
        }

        @Override
        protected int getIdColumnIdx(Cursor c) {
            return c.getColumnIndexOrThrow(BaseColumns._ID);
        }

        @Override
        protected int getNameColumnIdx(Cursor c) {
            return c.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME);
        }

        @Override
        protected Uri getUriForId(long id) {
            return Uris.PLAYLIST(id);
        }

        @Override
        protected Playlist createItem(long id, String name, int songCount, int albumCount, long[] songIds, long[] albumIds) {
            return new Playlist(id, name, songCount, albumCount, songIds, albumIds);
        }

        @Override
        public Observable<Playlist> getCollection() {

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

            Observable<Playlist> lastAddedObservable = performSomeMagick(
                    // performSomeMagick subscribes us on io so we dont need to do that here
                    lastAddedLoader.createObservable(),
                    -1, context.getResources().getString(R.string.playlist_last_added));
                //TODO instead of merge is there some kind of onComplete do, so that lastadded is always first
            return Observable.mergeDelayError(lastAddedObservable, super.getCollection()).observeOn(AndroidSchedulers.mainThread());
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
