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

package org.opensilk.music.ui3.nowplaying;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.ActionBarConfig;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortar.Lifecycle;
import org.opensilk.common.ui.mortar.LifecycleService;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.common.ui.mortar.ToolbarOwner;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.UtilsCommon;
import org.opensilk.music.ui3.main.ProgressUpdater;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.support.v4.media.MediaMetadataCompat.*;
import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class NowPlayingScreenPresenter extends ViewPresenter<NowPlayingScreenView> {

    final Context appContext;
    final PlaybackController playbackController;
    final ArtworkRequestManager requestor;
    final AppPreferences settings;
    final DrawerOwner drawerController;

    Observable<Lifecycle> lifecycle;
    CompositeSubscription broadcastSubscription;
    Subscription lifcycleSubscripton;

    boolean isPlaying;
    int sessionId = 0;
    Uri lastArtUri;
    String lastTrack;
    String lastArtist;
    PlaybackStateCompat lastState;

    final ProgressUpdater mProgressUpdater = new ProgressUpdater(new Action1<Integer>() {
        @Override
        public void call(Integer integer) {
            setProgress(integer);
        }
    });

    @Inject
    public NowPlayingScreenPresenter(
            @ForApplication Context appContext,
            PlaybackController playbackController,
            ArtworkRequestManager requestor,
            AppPreferences settings,
            DrawerOwner drawerController
    ) {
        this.appContext = appContext;
        this.playbackController = playbackController;
        this.requestor = requestor;
        this.settings = settings;
        this.drawerController = drawerController;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        lifecycle = LifecycleService.getLifecycle(scope);
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (lastArtUri != null) {
            loadArtwork(lastArtUri);
        }
        setCurrentTrack(lastTrack);
        setCurrentArtist(lastArtist);
        if (lastState != null) {
            onNewPlaybackState(lastState);
        }
        getView().setPlaying(false);
        if (lifcycleSubscripton != null) {
            lifcycleSubscripton.unsubscribe();
        }
        lifcycleSubscripton = lifecycle.subscribe(new Action1<Lifecycle>() {
            @Override
            @DebugLog
            public void call(Lifecycle lifecycle) {
                switch (lifecycle) {
                    case RESUME: {
                        subscribeBroadcasts();
                        //progressupdater is kicked off by subscriptions
                        break;
                    }
                    case PAUSE: {
                        unsubscribeBroadcasts();
                        mProgressUpdater.unsubscribeProgress();
                        destroyVisualizer();
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (lifcycleSubscripton != null) {
            lifcycleSubscripton.unsubscribe();
        }
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)){
            return;
        }
        Subscription s1 = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        lastState = playbackState;
                        onNewPlaybackState(playbackState);
                    }
                }
        );
        Subscription s2 = playbackController.subscribeMetaChanges(
                new Action1<MediaMetadataCompat>() {
                    @Override
                    public void call(MediaMetadataCompat mediaMetadata) {
                        String uriString = mediaMetadata.getString(METADATA_KEY_ALBUM_ART_URI);
                        if (!StringUtils.isEmpty(uriString)) {
                            lastArtUri = Uri.parse(uriString);
                            loadArtwork(lastArtUri);
                        }
                        String track = mediaMetadata.getString(METADATA_KEY_TITLE);
                        String artist = mediaMetadata.getString(METADATA_KEY_ARTIST);
                        lastTrack = track;
                        setCurrentTrack(track);
                        lastArtist = artist;
                        setCurrentArtist(artist);
                    }
                }
        );
        Subscription s3 = playbackController.subscribeAudioSessionIdChanges(
                new Action1<Integer>() {
                    @Override
                    public void call(Integer integer) {
                        Timber.d("New Session id %d", integer);
                        sessionId = integer;
                        updateVisualizer();
                    }
                }
        );
        broadcastSubscription = new CompositeSubscription(s1, s2, s3);
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)) {
            broadcastSubscription.unsubscribe();
            broadcastSubscription = null;
        }
    }

    void loadArtwork(Uri artUri) {
        if (hasView() && getView().getArtwork() != null) {
            requestor.newRequest(artUri, getView().getArtwork(), getView().mListener, null);
        }
    }

    void onNewPlaybackState(PlaybackStateCompat playbackState) {
        Timber.d("New playbackState %s", playbackState);
        isPlaying = PlaybackStateHelper.isPlaying(playbackState.getState());
        if (hasView()) {
            getView().setPlayChecked(PlaybackStateHelper.
                    shouldShowPauseButton(playbackState.getState()));
            getView().setPlaying(isPlaying);
        }
        mProgressUpdater.subscribeProgress(playbackState);
    }

    void setProgress(int progress) {
        if (hasView()) {
            getView().progress.setProgress(progress);
        }
    }

    void setCurrentTrack(CharSequence text) {
        if (hasView()) {
            getView().setCurrentTrack(text);
        }
    }

    void setCurrentArtist(CharSequence text) {
        if (hasView()) {
            getView().setCurrentArtist(text);
        }
    }

    public void pokeVisRenderer() {
        if (hasView()) {
            getView().reInitRenderer();
        }
    }

    void updateVisualizer() {
        if (hasView()) {
            getView().relinkVisualizer(sessionId);
            getView().setPlaying(isPlaying);
        }
    }

    void destroyVisualizer() {
        if (hasView()) {
            getView().destroyVisualizer();
        }
    }

    void disableVisualizer() {
        if (hasView()) {
            getView().disableVisualizer();;
        }
    }

    ActionBarConfig getActionBarConfig() {
        return ActionBarConfig.builder()
                .setTitle("")
                .setMenuConfig(new NowPlayingScreenMenuHander(this))
                .build();
    }

}
