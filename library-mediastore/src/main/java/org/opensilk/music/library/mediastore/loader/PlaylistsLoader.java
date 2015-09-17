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

import android.content.Context;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.rx.RxCursorLoader;
import org.opensilk.music.library.mediastore.provider.MediaStoreLibraryProvider;
import org.opensilk.music.library.mediastore.util.Projections;
import org.opensilk.music.library.mediastore.util.SelectionArgs;
import org.opensilk.music.library.mediastore.util.Selections;
import org.opensilk.music.library.mediastore.util.Uris;
import org.opensilk.music.library.provider.LibraryUris;
import org.opensilk.music.model.Playlist;
import org.opensilk.music.model.Track;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import rx.Observable;
import rx.functions.Action2;
import rx.functions.Func1;

import static org.opensilk.music.library.mediastore.util.CursorHelpers.generateArtworkUri;
import static org.opensilk.music.library.mediastore.util.CursorHelpers.getStringOrEmpty;

/**
 * Created by drew on 5/4/15.
 */
public class PlaylistsLoader extends RxCursorLoader<Playlist> {

    final String myAuthority;
    final Provider<TracksLoader> tracksLoaderProvider;

    @Inject
    public PlaylistsLoader(
            @ForApplication Context context,
            @Named("mediaStoreLibraryBaseAuthority") String baseAuthority,
            Provider<TracksLoader> tracksLoaderProvider
    ) {
        super(context);
        this.myAuthority = MediaStoreLibraryProvider.AUTHORITY_PFX+baseAuthority;
        this.tracksLoaderProvider = tracksLoaderProvider;
        setUri(Uris.EXTERNAL_MEDIASTORE_PLAYLISTS);
        setProjection(Projections.PLAYLIST);
        setSelection(Selections.PLAYLIST);
        setSelectionArgs(SelectionArgs.PLAYLIST);
        // need set sortorder
    }

    @Override
    protected Playlist makeFromCursor(Cursor c) throws Exception {
        final String id = c.getString(c.getColumnIndexOrThrow(BaseColumns._ID));
        final String name= getStringOrEmpty(c, MediaStore.Audio.Playlists.NAME);
        return Playlist.builder()
                .setUri(LibraryUris.playlist(myAuthority, "0", id))
                .setName(name)
                .build();
    }

    @Override
    public Observable<Playlist> createObservable() {
        return super.createObservable().flatMap(new Func1<Playlist, Observable<Playlist>>() {
            @Override
            public Observable<Playlist> call(Playlist playlist) {
                //We have to pull all the member tracks so we can
                //set the extra info.
                TracksLoader l = tracksLoaderProvider.get();
                l.setUri(Uris.PLAYLIST_MEMBERS(playlist.getUri().getLastPathSegment()));
                l.setProjection(Projections.PLAYLIST_SONGS);
                l.setSelection(Selections.PLAYLIST_SONGS);
                l.setSelectionArgs(SelectionArgs.PLAYLIST_SONGS);
                l.setSortOrder(MediaStore.Audio.Playlists.Members.PLAY_ORDER);
//                return l.createObservable().collect(playlist.buildUpon(), new Action2<Playlist.Builder, Track>() {
//                    @Override
//                    public void call(Playlist.Builder builder, Track track) {
//                        builder.addTrackUri(LibraryUris.track(myAuthority, "0", track.identity));
//                        builder.addArtInfo(track.albumArtistName, track.artistName, generateArtworkUri(track.albumIdentity));
//                    }
//                }).map(new Func1<Playlist.Builder, Playlist>() {
//                    @Override
//                    public Playlist call(Playlist.Builder builder) {
//                        return builder.build();
//                    }
//                });
                return Observable.empty();
            }
        });
    }

}
