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

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.View;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.ui.mortar.DrawerOwner;
import org.opensilk.common.ui.mortar.Lifecycle;
import org.opensilk.common.ui.mortar.LifecycleService;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.common.UtilsCommon;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 9/19/15.
 */
@ScreenScope
public class ControlsScreenPresenter extends ViewPresenter<ControlsScreenView>
        implements DrawerLayout.DrawerListener {

    final PlaybackController playbackController;
    final DrawerOwner drawerOwner;

    Observable<Lifecycle> lifecycle;
    Subscription lifecycleSubscripton;

    CompositeSubscription broadcastSubscription;
    Subscription blinkingSubscription;

    long posOverride = -1;
    long lastSeekEventTime;
    boolean fromTouch = false;
    boolean isPlaying;
    long lastBlinkTime;
    boolean drawerOpen;

    final ProgressUpdater mProgressUpdater = new ProgressUpdater(new Action1<Integer>() {
        @Override
        public void call(Integer integer) {
            setProgress(integer);
        }
    });

    @Inject
    public ControlsScreenPresenter(
            PlaybackController playbackController,
            DrawerOwner drawerOwner
    ) {
        this.playbackController = playbackController;
        this.drawerOwner = drawerOwner;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        lifecycle = LifecycleService.getLifecycle(scope);
        drawerOwner.register(scope, this);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        RxUtils.unsubscribe(lifecycleSubscripton);
        teardown();
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        RxUtils.unsubscribe(lifecycleSubscripton);
        lifecycleSubscripton = lifecycle.subscribe(new Action1<Lifecycle>() {
            @Override
            public void call(Lifecycle lifecycle) {
                switch (lifecycle) {
                    case RESUME:
                        subscribeBroadcasts();
                        break;
                    case PAUSE:
                        teardown();
                        break;
                }
            }
        });
    }

    void teardown() {
        unsubscribeBroadcasts();
        mProgressUpdater.unsubscribeProgress();
        unsubscribeBlinking();
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)){
            return;
        }
        Subscription s1 = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        if (hasView()) {
                            getView().setPlayChecked(PlaybackStateHelper.
                                    shouldShowPauseButton(playbackState.getState()));
                        }
                        mProgressUpdater.subscribeProgress(playbackState);
                        isPlaying = PlaybackStateHelper.isPlaying(playbackState.getState());
                        subscribeBlinking();
                    }
                }
        );
        Subscription s2 = playbackController.subscribeRepeatModeChanges(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                if (hasView()) {
                    getView().setRepeatLevel(integer);
                }
            }
        });
        Subscription s3 = playbackController.subscribeShuffleModeChanges(new Action1<Integer>() {
            @Override
            public void call(Integer integer) {
                if (hasView()) {
                    getView().setShuffleLevel(integer);
                }
            }
        });
        broadcastSubscription = new CompositeSubscription(s1, s2, s3);
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscription)) {
            broadcastSubscription.unsubscribe();
            broadcastSubscription = null;
        }
    }

    void subscribeBlinking() {
        if (!isPlaying && isSubscribed(blinkingSubscription)) {
            return;
        } else if (isPlaying) {
            unsubscribeBlinking();
            doBlinky();
            return;
        }
        final long interval = 250;
        blinkingSubscription = Observable.interval(interval,
                TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        doBlinky();
                    }
                });
    }

    void unsubscribeBlinking() {
        if (isSubscribed(blinkingSubscription)) {
            blinkingSubscription.unsubscribe();
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
            posOverride = (mProgressUpdater.getLastKnownDuration() * progress) / 1000;
            if (posOverride < 0 || !fromTouch) {
                posOverride = -1;
            } else {
                mProgressUpdater.setLastKnownPosition(posOverride);
                mProgressUpdater.setLastUpdateTime(now);
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
        }
        posOverride = -1;
    }
    /*end seekbars*/

    void setProgress(int progress) {
        if (!fromTouch) {
            doBlinky();
            if (hasView()) {
                getView().setProgress(progress);
            }
        }
    }

    void doBlinky() {
        if (!fromTouch) {
            setCurrentTimeText(mProgressUpdater.getLastFakedPosition());
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
            //TODO find somewhere to put this that doent update as much
            setTotalTimeText(mProgressUpdater.getLastKnownDuration());
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
