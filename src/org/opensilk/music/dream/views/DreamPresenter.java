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

package org.opensilk.music.dream.views;

import android.content.Context;
import android.os.Bundle;

import org.opensilk.music.api.meta.ArtInfo;
import org.opensilk.music.ui2.main.BroadcastObservables;
import org.opensilk.music.ui2.main.MusicServiceConnection;
import org.opensilk.silkdagger.qualifier.ForApplication;

import javax.inject.Inject;

import mortar.MortarScope;
import mortar.Presenter;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.notSubscribed;
import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 11/7/14.
 */
public class DreamPresenter extends Presenter<IDreamView> {

    final Context appContext;
    final MusicServiceConnection connection;

    CompositeSubscription broadcastSubscriptions;
    boolean isPlaying;
    int shuffleMode;
    int repeatMode;
    String track;
    String artist;
    String album;
    ArtInfo artInfo;

    @Inject
    public DreamPresenter(@ForApplication Context appContext,
                          MusicServiceConnection connection) {
        this.appContext = appContext;
        this.connection = connection;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        subscribeBroadcasts();
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        doFullUpdate();
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        unsubscribebroadcasts();
    }

    @Override
    protected MortarScope extractScope(IDreamView view) {
        return view.getScope();
    }

    void doFullUpdate() {
        updatePlaystate();
        updateShuffleState();
        updateRepeatState();
        updateTrack();
        updateArtist();
        updateAlbum();
        updateArtwork();
    }

    void updatePlaystate() {
        if (getView() == null) return;
        getView().updatePlaystate(isPlaying);
    }

    void updateShuffleState() {
        if (getView() == null) return;
        getView().updateShuffleState(shuffleMode);
    }

    void updateRepeatState() {
        if (getView() == null) return;
        getView().updateRepeatState(repeatMode);
    }

    void updateTrack() {
        if (getView() == null) return;
        getView().updateTrack(track);
    }

    void updateArtist() {
        if (getView() == null) return;
        getView().updateArtist(artist);
    }

    void updateAlbum() {
        if (getView() == null) return;
        getView().updateAlbum(album);
    }

    void updateArtwork() {
        if (artInfo == null) return;
        if (getView() == null) return;
        getView().updateArtwork(artInfo);
    }

    void subscribeBroadcasts() {
        if (notSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions = new CompositeSubscription(
                    observeOnMain(BroadcastObservables.playStateChanged(appContext)).subscribe(new Action1<Boolean>() {
                        @Override
                        public void call(Boolean playing) {
                            isPlaying = playing;
                            updatePlaystate();
                        }
                    }),
                    observeOnMain(BroadcastObservables.shuffleModeChanged(appContext, connection)).subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            shuffleMode = integer;
                            updateShuffleState();
                        }
                    }),
                    observeOnMain(BroadcastObservables.repeatModeChanged(appContext, connection)).subscribe(new Action1<Integer>() {
                        @Override
                        public void call(Integer integer) {
                            repeatMode = integer;
                            updateRepeatState();
                        }
                    }),
                    observeOnMain(BroadcastObservables.trackChanged(appContext)).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            track = s;
                            updateTrack();
                        }
                    }),
                    observeOnMain(BroadcastObservables.artistChanged(appContext)).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            artist = s;
                            updateArtist();
                        }
                    }),
                    observeOnMain(BroadcastObservables.albumChanged(appContext)).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            album = s;
                            updateAlbum();
                        }
                    }),
                    observeOnMain(BroadcastObservables.artworkChanged(appContext, connection)).subscribe(new Action1<ArtInfo>() {
                        @Override
                        public void call(ArtInfo artInfo) {
                            DreamPresenter.this.artInfo = artInfo;
                            updateArtwork();
                        }
                    })
            );
        }
    }

    void unsubscribebroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }
    }

}
