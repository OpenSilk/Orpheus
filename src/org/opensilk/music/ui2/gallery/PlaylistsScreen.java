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
import android.widget.PopupMenu;

import com.andrew.apollo.model.Playlist;
import com.andrew.apollo.utils.MusicUtils;
import com.andrew.apollo.utils.NavUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.common.PopupMenuHandler;
import org.opensilk.music.ui2.core.android.ActionBarOwner;
import org.opensilk.music.ui2.loader.RxLoader;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Provides;
import mortar.ViewPresenter;
import rx.functions.Action0;
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

        @Inject
        public Presenter(AppPreferences preferences, ArtworkRequestManager artworkRequestor, RxLoader<Playlist> loader) {
            super(preferences, artworkRequestor, loader);
        }

        @Override
        protected void load() {
            subscription = loader.getObservable().subscribe(new Action1<Playlist>() {
                @Override
                public void call(Playlist playlist) {
                    adapter.add(playlist);
                    showRecyclerView();
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {

                }
            }, new Action0() {
                @Override
                public void call() {
                    if (adapter.isEmpty()) showEmptyView();
                }
            });
        }

        @Override
        protected void handleItemClick(Context context, Playlist item) {
            NavUtils.openPlaylistProfile(context, item);
        }

        @Override
        protected BaseAdapter<Playlist> newAdapter() {
            return new Adapter(artworkRequestor);
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

    static class Adapter extends BaseAdapter<Playlist> {

        Adapter(ArtworkRequestManager artworkRequestor) {
            super(artworkRequestor);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final Playlist playlist = getItem(position);
            holder.title.setText(playlist.mPlaylistName);
            holder.subtitle.setText(MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, playlist.mSongNumber));
            holder.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu m = new PopupMenu(v.getContext(), v);
                    PopupMenuHandler.populateMenu(m, playlist);
                    m.show();
                }
            });
            switch (holder.artNumber) {
                case 4:
                    if (playlist.mAlbumIds.length >= 4) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork4,
                                playlist.mAlbumIds[3], ArtworkType.THUMBNAIL));
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork3,
                                playlist.mAlbumIds[2], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 2:
                    if (playlist.mAlbumIds.length >= 2) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork2,
                                playlist.mAlbumIds[1], ArtworkType.THUMBNAIL));
                    }
                    //fall
                case 1:
                    if (playlist.mAlbumIds.length >= 1) {
                        holder.subscriptions.add(artworkRequestor.newAlbumRequest((AnimatedImageView)holder.artwork,
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
