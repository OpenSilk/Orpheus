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

package org.opensilk.music.ui2.profile;

import android.os.Bundle;

import com.andrew.apollo.model.LocalSong;
import com.andrew.apollo.model.Playlist;

import org.opensilk.common.rx.SimpleObserver;
import org.opensilk.music.ui2.loader.LocalPlaylistSongLoader;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import mortar.ViewPresenter;
import rx.Subscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 4/20/15.
 */
@Singleton
public class PlaylistScreenPresenterDslv extends ViewPresenter<PlaylistDragSortView> {

    final Playlist playlist;
    final LocalPlaylistSongLoader loader;

    Subscription loaderSubscription;

    @Inject
    public PlaylistScreenPresenterDslv(Playlist playlist,
                                       LocalPlaylistSongLoader loader) {
        this.playlist = playlist;
        this.loader = loader;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (isSubscribed(loaderSubscription)) loaderSubscription.unsubscribe();
        loaderSubscription = loader.getListObservable().subscribe(new SimpleObserver<List<LocalSong>>() {
            @Override
            public void onNext(List<LocalSong> localSongs) {
                if (getView() != null) {
                    getView().mAdapter.addAll(localSongs);
                }
            }
        });
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (isSubscribed(loaderSubscription)) {
            loaderSubscription.unsubscribe();
            loaderSubscription = null;
        }
    }

    boolean isLastAdded() {
        return playlist.mPlaylistId == -2;
    }

}
