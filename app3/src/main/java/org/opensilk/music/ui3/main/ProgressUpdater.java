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

import android.os.SystemClock;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.common.core.util.BundleHelper;
import org.opensilk.common.core.util.VersionUtils;
import org.opensilk.music.playback.PlaybackStateHelper;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;
import static org.opensilk.common.core.rx.RxUtils.unsubscribe;

/**
 * Created by drew on 9/30/15.
 */
public class ProgressUpdater {
    private final Action1<Integer> setAction;
    private Subscription progressSubscription;
    private long lastUpdateTime;
    private long lastKnownPosition;
    private long lastKnownDuration;
    private long lastFakedPosition;

    public ProgressUpdater(Action1<Integer> setAction) {
        this.setAction = setAction;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public void setLastKnownPosition(long lastKnownPosition) {
        this.lastKnownPosition = lastKnownPosition;
        this.lastFakedPosition = lastKnownPosition;
    }

    public long getLastKnownPosition() {
        return lastKnownPosition;
    }

    public void setLastKnownDuration(long lastKnownDuration) {
        this.lastKnownDuration = lastKnownDuration;
    }

    public long getLastKnownDuration() {
        return lastKnownDuration;
    }

    public long getLastFakedPosition() {
        return lastFakedPosition;
    }

    private void doUpdate() {
        if (lastKnownPosition <= 0 || lastKnownDuration <= 0) {
            setAction.call(1001);
        } else {
            lastFakedPosition = lastKnownPosition +
                    (SystemClock.elapsedRealtime() - lastUpdateTime);
            setAction.call((int) (1000 * lastFakedPosition / lastKnownDuration));
        }
    }

    public void subscribeProgress(PlaybackStateCompat state) {
        if (PlaybackStateHelper.isLoading(state.getState())) {
            setLastKnownPosition(-1);
            setLastKnownDuration(-1);
            setLastUpdateTime(-1);
        } else {
            long position = state.getPosition();
            long duration;
            if (VersionUtils.hasApi22()) {
                duration = BundleHelper.getLong(state.getExtras());
            } else {
                duration = state.getBufferedPosition();
            }
            if (position < 0 || duration <= 0) {
                setLastKnownPosition(-1);
                setLastKnownDuration(-1);
            } else {
                setLastKnownPosition(position);
                setLastKnownDuration(duration);
            }
            setLastUpdateTime(state.getLastPositionUpdateTime());
        }
        if (!PlaybackStateHelper.isPlaying(state.getState())) {
            unsubscribeProgress();
            if (PlaybackStateHelper.isLoading(state.getState())) {
                setAction.call(-1);
            } else {
                doUpdate();
            }
        } else if (!isSubscribed(progressSubscription))  {
            final long interval = 250;
            progressSubscription = Observable.interval(interval, TimeUnit.MILLISECONDS,
                    AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
                @Override
                public void call(Long aLong) {
                    doUpdate();
                }
            });
        }
    }

    public void unsubscribeProgress() {
        unsubscribe(progressSubscription);
        progressSubscription = null;
    }
}
