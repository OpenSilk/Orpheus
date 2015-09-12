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

package org.opensilk.music.index.provider;

import android.content.ComponentName;
import android.os.Bundle;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.index.IndexComponent;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.provider.LibraryProviderOld;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import java.util.List;

import javax.inject.Named;

import rx.Subscriber;
import timber.log.Timber;

/**
 * Created by drew on 7/11/15.
 */
public class IndexProvider extends LibraryProviderOld {
    static final boolean TESTING = false; //for when the tester app doesnt use mortar

    @Named("IndexProviderAuthority") String mRealAuthority;

    @Override
    public boolean onCreate() {
        final AppContextComponent acc;
        if (TESTING) {
            Timber.plant(new Timber.DebugTree());
            acc = AppContextComponent.FACTORY.call(getContext());
        } else {
            acc = DaggerService.getDaggerComponent(getContext());
        }
        IndexComponent.FACTORY.call(acc).inject(this);
        super.onCreate();
        //override authority to avoid discover-ability
        mAuthority = mRealAuthority;
        return true;
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .setAuthority(mAuthority)
                //Not used but can't be null.
                .setLabel("index")
                .build();
    }

    protected void getAlbums(Subscriber<? super List<Album>> subscriber, Bundle args) {

    }

    protected void getAlbum(Subscriber<? super Album> subscriber, Bundle args) {

    }

    protected void getAlbumTracks(Subscriber<? super List<Track>> subscriber, Bundle args) {

    }

    protected void getArtists(Subscriber<? super List<Artist>> subscriber, Bundle args) {

    }

    protected void getArtist(Subscriber<? super Artist> subscriber, Bundle args) {

    }

    protected void getArtistAlbums(Subscriber<? super List<Album>> subscriber, Bundle args) {

    }

    protected void getArtistTracks(Subscriber<? super List<Track>> subscriber, Bundle args) {

    }

    protected void getGenres(Subscriber<? super List<Genre>> subscriber, Bundle args) {

    }

    protected void getGenre(Subscriber<? super Genre> subscriber, Bundle args) {

    }

    protected void getGenreAlbums(Subscriber<? super List<Album>> subscriber, Bundle args) {

    }

    protected void getGenreTracks(Subscriber<? super List<Track>> subscriber, Bundle args) {

    }

    protected void getPlaylists(Subscriber<? super List<Playlist>> subscriber, Bundle args) {

    }

    protected void getPlaylist(Subscriber<? super Playlist> subscriber, Bundle args) {

    }

    protected void getPlaylistTracks(Subscriber<? super List<Track>> subscriber, Bundle args) {

    }

    protected void getTracks(Subscriber<? super List<Track>> subscriber, Bundle args) {

    }

    protected void getTrack(Subscriber<? super Track> subscriber, Bundle args) {

    }

}
