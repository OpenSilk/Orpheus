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
import android.view.View;

import com.andrew.apollo.model.Genre;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.OverflowHandlers;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import mortar.ViewPresenter;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(GenresScreen.Module.class)
public class GenresScreen extends Screen {

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
    public static class Presenter extends BasePresenter<Genre> {

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor,
                         RxLoader<Genre> loader, OverflowHandlers.Genres popupHandler) {
            super(preferences, artworkRequestor, loader, popupHandler);
        }

        @Override
        protected void load() {
            subscription = loader.getObservable().subscribe(new Action1<Genre>() {
                @Override
                public void call(Genre genre) {
                    if (viewNotNull()) {
                        getAdapter().add(genre);
                        showRecyclerView();
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {

                }
            }, new Action0() {
                @Override
                public void call() {
                    if (viewNotNull() && getAdapter().isEmpty()) showEmptyView();
                }
            });
        }

        @Override
        protected void onItemClicked(View view, Genre item) {
            NavUtils.openGenreProfile(view.getContext(), item);
        }

        @Override
        protected BaseAdapter<Genre> newAdapter() {
            return new Adapter(this, artworkRequestor);
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

    static class Adapter extends BaseAdapter<Genre> {

        Adapter(BasePresenter<Genre> presenter, ArtworkRequestManager artworkRequestor) {
            super(presenter, artworkRequestor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, Genre genre) {
            holder.title.setText(genre.mGenreName);
            Context context = holder.itemView.getContext();
            String l2 = MusicUtils.makeLabel(context, R.plurals.Nalbums, genre.mAlbumNumber)
                    + ", " + MusicUtils.makeLabel(context, R.plurals.Nsongs, genre.mSongNumber);
            holder.subtitle.setText(l2);
            switch (holder.artNumber) {
                case 4:
                    if (genre.mAlbumIds.length >= 4) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork4,
                                null, genre.mAlbumIds[3], ArtworkType.THUMBNAIL));
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork3,
                                null, genre.mAlbumIds[2], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 2:
                    if (genre.mAlbumIds.length >= 2) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork2,
                                null, genre.mAlbumIds[1], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 1:
                    if (genre.mAlbumIds.length >= 1) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork,
                                null, genre.mAlbumIds[0], ArtworkType.THUMBNAIL));
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
