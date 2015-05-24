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

package org.opensilk.music.ui3.main;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.apache.commons.lang3.StringUtils;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.nowplaying.NowPlayingActivity;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static android.support.v4.media.MediaMetadataCompat.*;
import static android.support.v4.media.session.PlaybackStateCompat.*;


/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class FooterScreenPresenter extends ViewPresenter<FooterScreenView> implements PausesAndResumes {

    final Context appContext;
    final PlaybackController playbackController;
    final ArtworkRequestManager artworkReqestor;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final AppPreferences settings;

    String lastTrackName;
    String lastArtistName;
    Bitmap lastAlbumARt;
    long lastPosition;
    long lastDuration;
    boolean lastPosSynced;

    CompositeSubscription broadcastSubscriptions;
    Subscription progressSubscription;

    @Inject
    public FooterScreenPresenter(
            @ForApplication Context context,
            PlaybackController playbackController,
            ArtworkRequestManager artworkReqestor,
            PauseAndResumeRegistrar pauseAndResumeRegistrar,
            AppPreferences settings
    ) {
        this.appContext = context;
        this.playbackController = playbackController;
        this.artworkReqestor = artworkReqestor;
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        this.settings = settings;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        Timber.v("onEnterScope()");
        super.onEnterScope(scope);
        pauseAndResumeRegistrar.register(scope, this);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        Timber.v("onLoad()");
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onResume()");
            init();
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        Timber.v("onSave()");
        super.onSave(outState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onPause()");
            unsubscribeBroadcasts();
            unsubscribeProgress();
        }
    }

    @Override
    public void onResume() {
        Timber.v("onResume()");
        if (hasView()) {
            init();
        }
    }

    @Override
    public void onPause() {
        Timber.v("onPause");
        unsubscribeBroadcasts();
        unsubscribeProgress();
    }

    void init() {
        if (!StringUtils.isEmpty(lastTrackName)) {
            getView().trackTitle.setText(lastTrackName);
        }
        if (!StringUtils.isEmpty(lastArtistName)) {
            getView().artistName.setText(lastArtistName);
        }
        if (lastAlbumARt != null) {
            getView().artworkThumbnail.setImageBitmap(lastAlbumARt, false);
        }
        //progress is always updated
        subscribeBroadcasts();
    }

    void setTrackName(String s) {
        if (!StringUtils.equals(lastTrackName, s)) {
            lastTrackName = s;
            if (hasView()) {
                getView().trackTitle.setText(s);
            }
        }
    }

    void setArtistName(String s) {
        if (!StringUtils.equals(lastArtistName, s)) {
            lastArtistName = s;
            if (hasView()) {
                getView().artistName.setText(s);
            }
        }
    }

    void setProgress(int progress) {
        if (hasView()) {
            getView().progressBar.setIndeterminate(progress == -1);
            getView().progressBar.setProgress(progress);
        }
    }

    void updateArtwork(Bitmap bitmap) {
        if (bitmap != null) {
            if (!bitmap.sameAs(lastAlbumARt)) {
                lastAlbumARt = bitmap;
                if (hasView()) {
                    getView().artworkThumbnail.setImageBitmap(bitmap, true);
                }
            }
        } else if (hasView()) {
            getView().artworkThumbnail.setDefaultImage(R.drawable.default_artwork);
        }
    }

    void handleClick(Context context) {
        NowPlayingActivity.startSelf(context);
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            return;
        }
        Subscription s = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        final int state = playbackState.getState();
                        if (state == STATE_BUFFERING || state == STATE_CONNECTING) {
                            setProgress(-1);
                        } else {
                            long position = playbackState.getPosition();
                            long duration = playbackState.getBufferedPosition();
                            if (position < 0 || duration <= 0) {
                                setProgress(1000);
                            } else {
                                setProgress((int) (1000 * position / duration));
                            }
                            Timber.v("Position discrepancy = %d", lastPosition - position);
                            lastPosition = position;
                            lastDuration = duration;
                            lastPosSynced = true;
                            subscribeProgress(state == STATE_PLAYING);
                        }
                    }
                }
        );
        Subscription s2 = playbackController.subscribeMetaChanges(
                new Action1<MediaMetadataCompat>() {
                    @Override
                    public void call(MediaMetadataCompat mediaMetadata) {
                        setTrackName(mediaMetadata.getString(METADATA_KEY_TITLE));
                        setArtistName(mediaMetadata.getString(METADATA_KEY_ARTIST));
                        updateArtwork(mediaMetadata.getBitmap(METADATA_KEY_ALBUM_ART));
                    }
                }
        );
        broadcastSubscriptions = new CompositeSubscription(s, s2);
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }
    }

    void subscribeProgress(boolean playing) {
        if (!playing) {
            unsubscribeProgress();
            return;
        } else if (isSubscribed(progressSubscription))  {
            return;
        }
        final long interval = 250;
        progressSubscription = Observable.interval(interval, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        long position = lastPosition;
                        if (lastPosSynced) {
                            lastPosSynced = false;
                        } else {
                            position += interval + 10;
                            lastPosition = position;
                        }
                        long duration = lastDuration;
                        if (position < 0 || duration <= 0) {
                            setProgress(1000);
                        } else {
                            setProgress((int) (1000 * position / duration));
                        }
                    }
                });
    }

    void unsubscribeProgress() {
        if (isSubscribed(progressSubscription)) {
            progressSubscription.unsubscribe();
            progressSubscription = null;
        }
    }

}
