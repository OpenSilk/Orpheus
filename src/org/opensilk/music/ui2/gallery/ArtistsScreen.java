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
import android.os.Bundle;

import com.andrew.apollo.model.LocalArtist;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.common.flow.Screen;
import org.opensilk.common.mortar.WithModule;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.artwork.ArtworkManager;
import org.opensilk.music.ui2.loader.MediaStoreLoader;
import org.opensilk.music.util.CursorHelpers;
import org.opensilk.music.util.Projections;
import org.opensilk.music.util.SelectionArgs;
import org.opensilk.music.util.Selections;
import org.opensilk.music.util.Uris;
import org.opensilk.silkdagger.qualifier.ForApplication;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.functions.Action1;
import timber.log.Timber;

/**
 * Created by drew on 10/19/14.
 */
@WithModule(ArtistsScreen.Module.class)
@WithRecyclerViewPresenter(ArtistsScreen.Presenter.class)
public class ArtistsScreen extends Screen {

    @dagger.Module(
            addsTo = GalleryScreen.Module.class,
            injects = Presenter.class
    )
    public static class Module {

    }

    @Singleton
    public static class Presenter extends BasePresenter {

        Loader loader;

        @Inject
        public Presenter(AppPreferences preferences, Loader loader) {
            super(preferences);
            Timber.v("new ArtistsScreen.Presenter()");
            this.loader = loader;
        }

        @Override
        protected void onLoad(Bundle savedInstanceState) {
            Timber.v("onLoad()");
            super.onLoad(savedInstanceState);
            subscription = loader.getObservable().subscribe(new Action1<List<LocalArtist>>() {
                @Override
                public void call(List<LocalArtist> localArtists) {
                    setAdapter(new Adapter(localArtists));
                }
            });
        }

        @Override
        protected boolean isGrid() {
            return false;
        }

        @Override
        protected boolean isStaggered() {
            return false;
        }

    }

    @Singleton
    public static class Loader extends MediaStoreLoader<LocalArtist> {

        @Inject
        public Loader(@ForApplication Context context) {
            super(context);
            setUri(Uris.EXTERNAL_MEDIASTORE_ARTISTS);
            setProjection(Projections.LOCAL_ARTIST);
            setSelection(Selections.LOCAL_ARTIST);
            setSelectionArgs(SelectionArgs.LOCAL_ARTIST);
            //must set sort order
        }

        @Override
        protected LocalArtist makeFromCursor(Cursor c) {
            return CursorHelpers.makeLocalArtistFromCursor(c);
        }

    }

    static class Adapter extends BaseAdapter<LocalArtist> {

        Adapter(List<LocalArtist> items) {
            super(items);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            LocalArtist artist = items.get(position);
            holder.title.setText(artist.name);
            String subtitle = "";
            if (artist.albumCount > 0) {
                subtitle = MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nalbums, artist.albumCount);
            }
            if (artist.songCount > 0) {
                subtitle += MusicUtils.makeLabel(holder.itemView.getContext(), R.plurals.Nsongs, artist.songCount);
            }
            holder.subtitle.setText(subtitle);
            ArtworkManager.loadImage(new ArtInfo(artist.name, null, null), holder.artwork);
        }
    }

}
