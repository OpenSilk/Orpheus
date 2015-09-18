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

package org.opensilk.music.library.mediastore.loader;

import android.provider.BaseColumns;

import org.opensilk.music.model.Album;
import org.opensilk.music.model.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;

import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func0;
import rx.functions.Func1;

/**
 * Hijacks track observable streams and injects the albumartist into the track.
 *
 * I wrote this then found the hidden album_artist column... grr
 *
 * Created by drew on 5/4/15.
 */
public class AddAlbumArtistTrackTranformer implements Observable.Transformer<Track, Track> {

    final Provider<AlbumsLoader> mAlbumsLoaderProvider;

    public AddAlbumArtistTrackTranformer(Provider<AlbumsLoader> mAlbumsLoaderProvider) {
        this.mAlbumsLoaderProvider = mAlbumsLoaderProvider;
    }

    private static class State {
        final Set<String> albumIds = new HashSet<>();
        final List<Track> tracks = new ArrayList<>();
    }

    private static class AlbumArtistsMap extends HashMap<String, String> {
        public AlbumArtistsMap(int capacity) {
            super(capacity);
        }
    }

    @Override
    public Observable<Track> call(Observable<Track> trackObservable) {
        return trackObservable.collect(new Func0<State>() {
            @Override
            public State call() {
                return new State();
            }
        }, new Action2<State, Track>() {
            @Override
            public void call(State state, Track track) {
                //First collect all the albumids, and store the tracks for later use
                state.albumIds.add(track.getAlbumUri().getLastPathSegment());
                state.tracks.add(track);
            }
        }).flatMap(new Func1<State, Observable<Track>>() {
            @Override
            public Observable<Track> call(State state) {
                final State finalState = state;
                //obtain an albums loader, and replace the selection
                //with restriction on the album ids we care about
                AlbumsLoader l = mAlbumsLoaderProvider.get();
                l.setSelection(null);
                l.setSelectionArgs(null);
                final int size = state.albumIds.size();
                final StringBuilder selection = new StringBuilder();
                selection.append(BaseColumns._ID + " IN (");
                int ii = 0;
                for (String albumId : state.albumIds) {
                    selection.append(albumId);
                    if (++ii < size) {
                        selection.append(",");
                    }
                }
                selection.append(")");
                l.setSelection(selection.toString());
                //we query the mediastore for all the album ids we collected
                //then grab the albumartist from the albums and store in a map
                return l.createObservable().collect(new Func0<AlbumArtistsMap>() {
                    @Override
                    public AlbumArtistsMap call() {
                        return new AlbumArtistsMap(size);
                    }
                }, new Action2<AlbumArtistsMap, Album>() {
                    @Override
                    public void call(AlbumArtistsMap albumArtistsMap, Album album) {
                        albumArtistsMap.put(album.getUri().getLastPathSegment(), album.getArtistName());
                    }
                })
                //now get the tracks out of storage and add the albumartist to them
                //then reemmit
                .flatMap(new Func1<AlbumArtistsMap, Observable<Track>>() {
                    @Override
                    public Observable<Track> call(AlbumArtistsMap albumArtistsMap) {
                        //TODO could this be more idiomatic?
                        List<Track> l = new ArrayList<Track>(finalState.tracks.size());
                        for (Track t : finalState.tracks) {
                            l.add(t.buildUpon().setAlbumArtistName(albumArtistsMap.get(
                                    t.getAlbumUri().getLastPathSegment())).build());
                        }
                        return Observable.from(l);
                    }
                });
            }
        });
    }
}
