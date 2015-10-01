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

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

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

    void doUpdate() {
        if (lastKnownPosition <= 0 || lastKnownDuration <= 0) {
            setAction.call(1000);
        } else {
            lastFakedPosition = lastKnownPosition +
                    (SystemClock.elapsedRealtime() - lastUpdateTime);
            setAction.call((int) (1000 * lastFakedPosition / lastKnownDuration));
        }
    }

    void subscribeProgress(boolean playing) {
        if (!playing) {
            unsubscribeProgress();
            doUpdate();
            return;
        } else if (isSubscribed(progressSubscription))  {
            return;
        }
        final long interval = 250;
        progressSubscription = Observable.interval(interval, TimeUnit.MILLISECONDS,
                AndroidSchedulers.mainThread()).subscribe(new Action1<Long>() {
            @Override
            public void call(Long aLong) {
                doUpdate();
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
