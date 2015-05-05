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

package org.opensilk.music.library.mediastore.provider;

import android.content.ComponentName;
import android.os.Bundle;
import android.provider.MediaStore;

import org.opensilk.common.core.dagger2.AppContextComponent;
import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.library.LibraryConfig;
import org.opensilk.music.library.mediastore.MediaStoreLibraryComponent;
import org.opensilk.music.library.mediastore.loader.AlbumsLoader;
import org.opensilk.music.library.mediastore.loader.ArtistsLoader;
import org.opensilk.music.library.mediastore.loader.GenresLoader;
import org.opensilk.music.library.mediastore.loader.PlaylistsLoader;
import org.opensilk.music.library.mediastore.loader.TracksLoader;
import org.opensilk.music.library.mediastore.ui.FakeStorageActivity;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.library.provider.LibraryProvider;
import org.opensilk.music.model.Album;
import org.opensilk.music.model.Artist;
import org.opensilk.music.model.Genre;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import hugo.weaving.DebugLog;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import timber.log.Timber;

import static org.opensilk.music.library.LibraryCapability.ALBUMS;
import static org.opensilk.music.library.LibraryCapability.ARTISTS;
import static org.opensilk.music.library.LibraryCapability.GENRES;
import static org.opensilk.music.library.LibraryCapability.PLAYLISTS;
import static org.opensilk.music.library.LibraryCapability.TRACKS;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.appendId;

/**
 * Created by drew on 4/26/15.
 */
public class MediaStoreLibraryProvider extends LibraryProvider {
    private static final boolean TESTING = false; //for when the tester app doesnt use mortar

    @Inject @Named("mediaStoreLibraryBaseAuthority") String mBaseAuthority;
    @Inject Provider<AlbumsLoader> mAlbumsLoaderProvider;
    @Inject Provider<ArtistsLoader> mArtistsLoaderProvider;
    @Inject Provider<TracksLoader> mTracksLoaderProvider;
    @Inject Provider<GenresLoader> mGenresLoaderProvider;
    @Inject Provider<PlaylistsLoader> mPlaylistsLoaderProvider;

    @Override
    public boolean onCreate() {
        final AppContextComponent acc;
        if (TESTING) {
            Timber.plant(new Timber.DebugTree());
            acc = AppContextComponent.FACTORY.call(getContext());
        } else {
            acc = DaggerService.getDaggerComponent(getContext());
        }
        MediaStoreLibraryComponent.FACTORY.call(acc).inject(this);
        return super.onCreate();
    }

    @Override
    protected LibraryConfig getLibraryConfig() {
        return LibraryConfig.builder()
                .setCapabilities(ALBUMS|ARTISTS|GENRES|PLAYLISTS|TRACKS)
                .setPickerComponent(new ComponentName(getContext(), FakeStorageActivity.class), null)
                .setAuthority(mAuthority)
                .build();
    }

    @Override
    protected String getBaseAuthority() {
        return mBaseAuthority;
    }

    @Override
    protected void queryAlbums(String library, Subscriber<? super Album> subscriber, Bundle args) {
        AlbumsLoader l = mAlbumsLoaderProvider.get();
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getAlbum(String library, String identity, Subscriber<? super Album> subscriber, Bundle args) {
        AlbumsLoader l = mAlbumsLoaderProvider.get();
        l.setUri(appendId(Uris.EXTERNAL_MEDIASTORE_ALBUMS, identity));
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getAlbumTracks(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        TracksLoader l = mTracksLoaderProvider.get();
        l.setSelection(Selections.LOCAL_ALBUM_SONGS);
        l.setSelectionArgs(SelectionArgs.LOCAL_ALBUM_SONGS(identity));

        l.createObservable()
                .doOnNext(new Action1<Track>() {
                    @Override
                    public void call(Track track) {
                        Timber.v("Track name=%s artist=%s albumArtist=%s", track.name, track.artistName, track.albumArtistName);
                    }
                }).subscribe(subscriber);
    }

    @Override
    protected void queryArtists(String library, Subscriber<? super Artist> subscriber, Bundle args) {
        ArtistsLoader l = mArtistsLoaderProvider.get();
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getArtist(String library, String identity, Subscriber<? super Artist> subscriber, Bundle args) {
        ArtistsLoader l = mArtistsLoaderProvider.get();
        l.setUri(appendId(Uris.EXTERNAL_MEDIASTORE_ARTISTS, identity));
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getArtistAlbums(String library, String identity, Subscriber<? super Album> subscriber, Bundle args) {
        AlbumsLoader l = mAlbumsLoaderProvider.get();
        l.setUri(Uris.EXTERNAL_MEDIASTORE_ARTISTS_ALBUMS(identity));
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getArtistTracks(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        TracksLoader l = mTracksLoaderProvider.get();
        l.setSelection(Selections.LOCAL_ARTIST_SONGS);
        l.setSelectionArgs(SelectionArgs.LOCAL_ARTIST_SONGS(identity));

        l.createObservable()
                .doOnNext(new Action1<Track>() {
                    @Override
                    public void call(Track track) {
                        Timber.v("Track name=%s artist=%s albumArtist=%s", track.name, track.artistName, track.albumArtistName);
                    }
                }).subscribe(subscriber);
    }

    @Override
    protected void queryGenres(String library, Subscriber<? super Genre> subscriber, Bundle args) {
        GenresLoader l = mGenresLoaderProvider.get();
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getGenre(String library, String identity, Subscriber<? super Genre> subscriber, Bundle args) {
        GenresLoader l = mGenresLoaderProvider.get();
        l.setUri(appendId(Uris.EXTERNAL_MEDIASTORE_GENRES, identity));
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getGenreAlbums(String library, String identity, Subscriber<? super Album> subscriber, Bundle args) {
        GenresLoader l = mGenresLoaderProvider.get();
        l.setUri(appendId(Uris.EXTERNAL_MEDIASTORE_GENRES, identity));
        l.createObservable().flatMap(new Func1<Genre, Observable<Album>>() {
            @Override
            public Observable<Album> call(Genre genre) {
                //Extract the albumuris and load them
                AlbumsLoader l = mAlbumsLoaderProvider.get();
                String[] ids = new String[genre.albumUris.size()];
                for (int ii=0; ii<genre.albumUris.size(); ii++) {
                    ids[ii] = genre.albumUris.get(ii).getLastPathSegment();
                }
                l.setSelection(Selections.LOCAL_ALBUMS(ids));
                return l.createObservable();
            }
        });
    }

    @Override
    protected void getGenreTracks(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        TracksLoader l = mTracksLoaderProvider.get();
        l.setUri(Uris.GENRE_MEMBERS(identity));
        l.setProjection(Projections.GENRE_SONGS);
        l.setSelection(Selections.GENRE_SONGS);
        l.setSelectionArgs(SelectionArgs.GENRE_SONGS);
        l.createObservable().doOnNext(new Action1<Track>() {
            @Override
            public void call(Track track) {
                Timber.v("Track name=%s artist=%s albumArtist=%s", track.name, track.artistName, track.albumArtistName);
            }
        }).subscribe(subscriber);
    }

    @Override
    protected void queryPlaylists(String library, Subscriber<? super Playlist> subscriber, Bundle args) {
        PlaylistsLoader l = mPlaylistsLoaderProvider.get();
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getPlaylist(String library, String identity, Subscriber<? super Playlist> subscriber, Bundle args) {
        PlaylistsLoader l = mPlaylistsLoaderProvider.get();
        l.setUri(appendId(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS, identity));
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getPlaylistTracks(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        TracksLoader l = mTracksLoaderProvider.get();
        l.setUri(Uris.PLAYLIST_MEMBERS(identity));
        l.setProjection(Projections.PLAYLIST_SONGS);
        l.setSelection(Selections.PLAYLIST_SONGS);
        l.setSelectionArgs(SelectionArgs.PLAYLIST_SONGS);
        l.setSortOrder(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
        l.createObservable().doOnNext(new Action1<Track>() {
            @Override
            public void call(Track track) {
                Timber.v("Track name=%s artist=%s albumArtist=%s", track.name, track.artistName, track.albumArtistName);
            }
        }).subscribe(subscriber);
    }

    @Override
    protected void queryTracks(String library, Subscriber<? super Track> subscriber, Bundle args) {
        TracksLoader l = mTracksLoaderProvider.get();
        l.createObservable().subscribe(subscriber);
    }

    @Override
    protected void getTrack(String library, String identity, Subscriber<? super Track> subscriber, Bundle args) {
        TracksLoader l = mTracksLoaderProvider.get();
        l.setUri(appendId(Uris.EXTERNAL_MEDIASTORE_MEDIA, identity));
        l.createObservable().doOnNext(new Action1<Track>() {
            @Override
            public void call(Track track) {
                Timber.v("Track name=%s artist=%s albumArtist=%s", track.name, track.artistName, track.albumArtistName);
            }
        }).subscribe(subscriber);
    }

}
