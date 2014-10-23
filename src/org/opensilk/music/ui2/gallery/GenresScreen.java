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

import com.andrew.apollo.model.Genre;
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
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.SortOrder;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(GenresScreen.Module.class)
@WithGalleryPageViewPresenter(GenresScreen.Presenter.class)
public class GenresScreen extends Screen {

    @dagger.Module(
            addsTo = GalleryScreen.Module.class,
            injects = Presenter.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends BasePresenter<Genre> {

        final Loader loader;

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor, Loader loader) {
            super(preferences, artworkRequestor);
            this.loader = loader;
        }

        @Override
        protected void load() {
            subscription = loader.getCollection().subscribe(new Action1<Genre>() {
                @Override
                public void call(Genre genre) {
                    addItem(genre);
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    //TODO
                }
            });
        }

        @Override
        protected void handleItemClick(Context context, Genre item) {
            NavUtils.openGenreProfile(context, item);
        }

        @Override
        protected BaseAdapter<Genre> newAdapter(List<Genre> items) {
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
    public static class Loader extends AbsGenrePlaylistLoader<Genre> {

        @Inject
        public Loader(@ForApplication Context context) {
            super(context);
            setUri(Uris.EXTERNAL_MEDIASTORE_GENRES);
            setProjection(Projections.GENRE);
            setSelection(Selections.GENRE);
            setSelectionArgs(SelectionArgs.GENRE);
            setSortOrder(MediaStore.Audio.Genres.DEFAULT_SORT_ORDER);

            setProjection2(Projections.GENRE_SONGS);
            setSelection2(Selections.GENRE_SONGS);
            setSelectionArgs2(SelectionArgs.GENRE_SONGS);
            setSortOrder2(SortOrder.GENRE_SONGS);
        }

        @Override
        protected int getIdColumnIdx(Cursor c) {
            return c.getColumnIndexOrThrow(BaseColumns._ID);
        }

        @Override
        protected int getNameColumnIdx(Cursor c) {
            return c.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME);
        }

        @Override
        protected Uri getUriForId(long id) {
            return Uris.GENRE(id);
        }

        @Override
        protected Genre createItem(long id, String name, int songCount, int albumCount, long[] songIds, long[] albumIds) {
            return new Genre(id, name, songCount, albumCount, songIds, albumIds);
        }

        @Override
        public Observable<Genre> getCollection() {
            return super.getCollection().filter(new Func1<Genre, Boolean>() {
                @Override
                public Boolean call(Genre genre) {
                    // mediastore doesnt cleanup old genres so
                    // we have to make sure not to add any that are empty
                    return genre.mSongNumber > 0;
                }
            }).observeOn(AndroidSchedulers.mainThread());
        }

    }

    static class Adapter extends BaseAdapter<Genre> {

        Adapter(List<Genre> items, ArtworkRequestManager artworkRequestor) {
            super(items, artworkRequestor);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            Genre genre = getItem(position);
            holder.title.setText(genre.mGenreName);
            Context context = holder.itemView.getContext();
            String l2 = MusicUtils.makeLabel(context, R.plurals.Nalbums, genre.mAlbumNumber)
                    + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, genre.mSongNumber);
            holder.subtitle.setText(l2);
            switch (holder.artNumber) {
                case 4:
                    if (genre.mAlbumIds.length >= 4) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork4,
                                genre.mAlbumIds[3], ArtworkType.THUMBNAIL));
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork3,
                                genre.mAlbumIds[2], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 2:
                    if (genre.mAlbumIds.length >= 2) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork2,
                                genre.mAlbumIds[1], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 1:
                    if (genre.mAlbumIds.length >= 1) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest(holder.artwork,
                                genre.mAlbumIds[0], ArtworkType.THUMBNAIL));
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
