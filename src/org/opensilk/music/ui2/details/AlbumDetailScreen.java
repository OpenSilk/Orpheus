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

package org.opensilk.music.ui2.details;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.andrew.apollo.model.LocalAlbum;
import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.dagger.qualifier.ForApplication;
import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.common.mortarflow.WithTransitions;
import org.opensilk.common.rx.RxUtils;
import org.opensilk.common.widget.AnimatedImageView;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkRequestManager;
import org.opensilk.music.artwork.ArtworkType;
import org.opensilk.music.ui2.BaseSwitcherActivity;
import org.opensilk.music.ui2.loader.RxCursorLoader;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import butterknife.ButterKnife;
import butterknife.InjectView;
import dagger.Provides;
import flow.Layout;
import mortar.ViewPresenter;
import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by drew on 11/8/14.
 */
@Layout(R.layout.sticky_header_layout_list)
@WithModule(AlbumDetailScreen.Module.class)
@WithTransitions(
        forward = { R.anim.slide_out_left, R.anim.slide_in_right },
        backward = { R.anim.slide_out_right, R.anim.slide_in_left }
)
public class AlbumDetailScreen extends Screen {

    final LocalAlbum album;

    public AlbumDetailScreen(LocalAlbum album) {
        this.album = album;
    }

    @dagger.Module(
            addsTo = BaseSwitcherActivity.Module.class,
            injects = StickyHeaderListView.class
    )
    public static class Module {
        final AlbumDetailScreen screen;

        public Module(AlbumDetailScreen screen) {
            this.screen = screen;
        }

        @Provides @Singleton
        public StickyHeaderListPresenter providePresenter(Presenter presenter) {
            return presenter;
        }

        @Provides @Singleton
        public RxCursorLoader<LocalSong> provideAlbumSongsLoader(AlbumSongsLoader loader) {
            return loader;
        }

        @Provides
        public LocalAlbum provideAlbum() {
            return screen.album;
        }
    }

    @Singleton
    public static class Presenter extends StickyHeaderListPresenter {

        final LocalAlbum album;
        final RxCursorLoader<LocalSong> loader;
        final ArtworkRequestManager requestor;

        Subscription artworkSubscription;

        @Inject
        public Presenter(LocalAlbum album,
                         RxCursorLoader<LocalSong> loader,
                         ArtworkRequestManager requestor) {
            this.album = album;
            this.loader = loader;
            this.requestor = requestor;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            super.onLoad(savedInstanceState);
            ImageView hero = ButterKnife.findById(getView(), R.id.hero_image);
            loader.getListObservable().subscribe(new Action1<List<LocalSong>>() {
                @Override
                public void call(List<LocalSong> localSongs) {
                    if (getView() != null) getView().<ListView>getListView()
                                .setAdapter(new Adapter(getView().getContext(), localSongs));
                }
            });
            artworkSubscription = requestor.newAlbumRequest((AnimatedImageView) hero, getView().getPaletteObserver(),
                    new ArtInfo(album.artistName, album.name, album.artworkUri), ArtworkType.LARGE);
        }

        @Override
        protected void onExitScope() {
            super.onExitScope();
            if (RxUtils.isSubscribed(artworkSubscription)) artworkSubscription.unsubscribe();
        }

        @Override
        public int getListHeaderLayout() {
            return R.layout.sticky_header_hero;
        }
    }

    static class Adapter extends ArrayAdapter<LocalSong> {

        LayoutInflater inflater;

        Adapter(Context context, List<LocalSong> songs) {
            super(context, -1, songs);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Holder holder;
            if (v == null) {
                if (inflater == null) inflater = LayoutInflater.from(getContext());
                v = inflater.inflate(R.layout.gallery_list_item_simple, parent, false);
                holder = new Holder(v);
                v.setTag(holder);
            } else {
                holder = (Holder) v.getTag();
            }
            LocalSong song = getItem(position);
            holder.title.setText(song.name);
            holder.subtitle.setText(song.artistName);
            holder.info.setText(MusicUtils.makeTimeString(getContext(), song.duration));
            holder.overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
            return v;
        }

        static class Holder {
            final View itemView;
            @InjectView(R.id.tile_title) TextView title;
            @InjectView(R.id.tile_subtitle) TextView subtitle;
            @InjectView(R.id.tile_info) TextView info;
            @InjectView(R.id.tile_overflow) ImageButton overflow;

            Holder(View itemView) {
                this.itemView = itemView;
                ButterKnife.inject(this, itemView);
            }

            public void reset() {

            }

        }

    }

    @Singleton
    static class AlbumSongsLoader extends RxCursorLoader<LocalSong> {

        @Inject
        AlbumSongsLoader(@ForApplication Context context, LocalAlbum album) {
            super(context);
            setUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
            setProjection(Projections.LOCAL_SONG);
            setSelection(Selections.LOCAL_ALBUM_SONGS);
            setSelectionArgs(SelectionArgs.LOCAL_ALBUM_SONGS(album.albumId));
            setSortOrder(Selections.LOCAL_ALBUM_SONGS);
        }

        @Override
        protected LocalSong makeFromCursor(Cursor c) {
            return CursorHelpers.makeLocalSongFromCursor(c);
        }
    }

}
