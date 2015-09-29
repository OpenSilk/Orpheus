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
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.model.ArtInfo;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.UtilsCommon;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static android.support.v4.media.session.PlaybackStateCompat.STATE_PLAYING;
import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 9/19/15.
 */
@ScreenScope
public class ControlsScreenPresenter extends ViewPresenter<ControlsScreenView>
        implements PausesAndResumes, DrawerLayout.DrawerListener {

    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final PlaybackController playbackController;
    final DrawerOwner drawerOwner;

    CompositeSubscription broadcastSubscription;

    long posOverride = -1;
    long lastSeekEventTime;
    boolean fromTouch = false;
    boolean isPlaying;
    long lastPosition = -1;
    long lastDuration = -1;
    boolean lastPosSynced;
    long lastBlinkTime;
    boolean drawerOpen;

    @Inject
    public ControlsScreenPresenter(
            PauseAndResumeRegistrar pauseAndResumeRegistrar,
            PlaybackController playbackController,
            DrawerOwner drawerOwner
    ) {
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
        this.playbackController = playbackController;
        this.drawerOwner = drawerOwner;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        pauseAndResumeRegistrar.register(scope, this);
        drawerOwner.register(scope, this);
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onResume()");
            setup();
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (pauseAndResumeRegistrar.isRunning()) {
            Timber.v("missed onPause()");
            teardown();
        }
    }

    @Override
    public void onResume() {
        if (hasView()) {
            Timber.v("missed onLoad()");
            setup();
        }
    }

    @Override
    public void onPause() {
        teardown();
    }

    @DebugLog
    void setup() {
        setCurrentTimeText(lastPosition);
        setTotalTimeText(lastDuration);
        subscribeBroadcasts();
    }

    @DebugLog
    void teardown() {
        unsubscribeBroadcasts();
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)){
            return;
        }
        Subscription s1 = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        boolean playing = playbackState.getState() == STATE_PLAYING;
                        if (hasView()) {
                            getView().setPlayChecked(PlaybackController.isPlayingOrSimilar(playbackState));
                            //TODO shuffle/repeat
                        }
                        isPlaying = playing;
                        long position = playbackState.getPosition();
                        long duration;
                        if (VersionUtils.hasApi22()) {
                            duration = BundleHelper.getLong(playbackState.getExtras());
                        } else {
                            duration = playbackState.getBufferedPosition();
                        }
                        updateProgress(position, duration);
                        if (duration != lastDuration) {
                            setTotalTimeText(duration);
                        }
                        Timber.v("Position discrepancy = %d", lastPosition - position);
                        lastPosition = position;
                        lastDuration = duration;
                        lastPosSynced = true;
                    }
                }
        );
        final long interval = 250;
        Subscription s3 = Observable.interval(interval, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        long position = lastPosition;
                        if (lastPosSynced) {
                            lastPosSynced = false;
                        } else if (isPlaying) {
                            position += interval + 10;
                            lastPosition = position;
                        }
                        long duration = lastDuration;
                        updateProgress(position, duration);
                    }
                });
        broadcastSubscription = new CompositeSubscription(s1, s3);
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)) {
            broadcastSubscription.unsubscribe();
            broadcastSubscription = null;
        }
    }

    /*drawer*/

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        if (!drawerOpen && hasView() && getView() == drawerView) {
            //TODO update progress
        }
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        if (hasView() && getView() == drawerView) {
            drawerOpen = true;
        }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        if (hasView() && getView() == drawerView) {
            drawerOpen = false;
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }

    /*end drawer*/

    /*seekbars*/
    public void onProgressChanged(final int progress, final boolean fromuser) {
        if (!fromuser) {
            return;
        }
        final long now = SystemClock.elapsedRealtime();
        if (now > lastSeekEventTime + 30) {
            lastSeekEventTime = now;
            posOverride = (lastDuration * progress) / 1000;
            if (posOverride < 0 || !fromTouch) {
                posOverride = -1;
            } else {
                setCurrentTimeText(posOverride);
            }
        }
    }

    public void onStartTrackingTouch() {
        lastSeekEventTime = 0;
        posOverride = -1;
        fromTouch = true;
        setCurrentTimeVisibile();
    }

    public void onStopTrackingTouch() {
        fromTouch = false;
        if (posOverride != -1) {
            playbackController.seekTo(posOverride);
            updateProgress(posOverride, lastDuration);
        }
        posOverride = -1;
    }
    /*end seekbars*/

    void updateProgress(long position, long duration) {
        if (position < 0 || duration <= 0) {
            setProgress(1000);
            setCurrentTimeText(-1);
        } else {
            if (!fromTouch) {
                setCurrentTimeText(position);
                setProgress((int) (1000 * position / duration));
                if (isPlaying) {
                    setCurrentTimeVisibile();
                } else {
                    //blink the counter
                    long now = SystemClock.elapsedRealtime();
                    if (now >= lastBlinkTime + 500) {
                        lastBlinkTime = now;
                        toggleCurrentTimeVisiblility();
                    }
                }
            }
        }
    }

    void setProgress(int progress) {
        if (hasView()) {
            getView().setProgress(progress);
        }
    }

    void setTotalTimeText(long duration) {
        if (hasView()) {
            if (duration > 0) {
                getView().setTotalTime(UtilsCommon.makeTimeString(getView().getContext(), duration / 1000));
            } else {
                getView().setTotalTime("--:--");
            }
        }
    }

    void setCurrentTimeText(long pos) {
        if (hasView()) {
            if (pos >= 0) {
                getView().setCurrentTime(UtilsCommon.makeTimeString(getView().getContext(), pos / 1000));
            } else {
                getView().setCurrentTime("--:--");
            }
        }
    }

    void setCurrentTimeVisibile() {
        if (hasView()) {
            getView().setCurrentTimeVisibility(View.VISIBLE);
        }
    }

    void toggleCurrentTimeVisiblility() {
        if (hasView()) {
            boolean visible = getView().getCurrentTimeVisibility() == View.VISIBLE;
            getView().setCurrentTimeVisibility(visible ? View.INVISIBLE : View.VISIBLE);
        }
    }

    PlaybackController getPlaybackController() {
        return playbackController;
    }

}
