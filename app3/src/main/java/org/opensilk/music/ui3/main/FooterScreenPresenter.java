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
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.core.rx.RxUtils;
import org.opensilk.common.ui.mortar.Lifecycle;
import org.opensilk.common.ui.mortar.LifecycleService;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.control.PlaybackController;

import java.util.ArrayList;
import java.util.List;
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

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;


/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class FooterScreenPresenter extends ViewPresenter<FooterScreenView> {

    final Context appContext;
    final PlaybackController playbackController;
    final AppPreferences settings;

    Observable<Lifecycle> lifecycle;
    Subscription lifecycleSub;
    CompositeSubscription broadcastSubscriptions;
    final ArrayList<FooterPageScreen> screens = new ArrayList<>();
    long lastPlayingId;
    boolean selfChange;

    final ProgressUpdater mProgressUpdater = new ProgressUpdater(new Action1<Integer>() {
        @Override
        public void call(Integer integer) {
            setProgress(integer);
        }
    });

    Subscription skipSubscription;
    final Observable<Long> skipToItemObservable = Observable.timer(700, TimeUnit.MILLISECONDS,
            AndroidSchedulers.mainThread());

    @Inject
    public FooterScreenPresenter(
            @ForApplication Context context,
            PlaybackController playbackController,
            AppPreferences settings
    ) {
        this.appContext = context;
        this.playbackController = playbackController;
        this.settings = settings;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        Timber.v("onEnterScope()");
        super.onEnterScope(scope);
        lifecycle = LifecycleService.getLifecycle(scope);
    }

    @Override
    protected void onExitScope() {
        Timber.v("onExitScope()");
        super.onExitScope();
        RxUtils.unsubscribe(lifecycleSub);
        teardown();
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        Timber.v("onLoad()");
        super.onLoad(savedInstanceState);
        if(!screens.isEmpty()) {
            getView().onNewItems(screens);
            updatePagerWithCurrentItem(lastPlayingId);
        }
        RxUtils.unsubscribe(lifecycleSub);
        lifecycleSub = lifecycle.subscribe(new Action1<Lifecycle>() {
            @Override
            @DebugLog
            public void call(Lifecycle lifecycle) {
                switch (lifecycle) {
                    case RESUME:
                        //progress is always updated
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
        RxUtils.unsubscribe(skipSubscription);
    }

    @DebugLog
    void skipToQueueItem(int pos) {
        if (pos >= 0  && pos < screens.size()) {
            final long id = screens.get(pos).queueItem.getQueueId();
            if (lastPlayingId != id) {
                lastPlayingId = id;
                RxUtils.unsubscribe(skipSubscription);
                //post on delay to allow skipping over multiple items
                skipSubscription = skipToItemObservable.subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        Timber.d("skippingToQueueItem %d", id);
                        selfChange = true;
                        playbackController.skipToQueueItem(id);
                    }
                });
            }
        }
    }

    @DebugLog
    void updatePagerWithCurrentItem(long id) {
        if (hasView()) {
            for (FooterPageScreen s : screens) {
                if (s.queueItem.getQueueId() == id) {
                    int idx = screens.indexOf(s);
                    getView().goTo(idx);
                }
            }
        }
    }

    void setProgress(int progress) {
        if (hasView()) {
            getView().progressBar.setIndeterminate(progress == -1);
            getView().progressBar.setProgress(progress);
        }
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            return;
        }
        final Subscription s1 = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        Timber.d("New PlaybackState %s", PlaybackStateHelper.stringifyState(playbackState.getState()));
                        mProgressUpdater.subscribeProgress(playbackState);
                        if (!selfChange) {
                            lastPlayingId = playbackState.getActiveQueueItemId();
                            updatePagerWithCurrentItem(lastPlayingId);
                        }
                    }
                }
        );
        Subscription s2 = playbackController.subscribeQueueChanges(
                new Action1<List<MediaSessionCompat.QueueItem>>() {
                    @Override
                    public void call(List<MediaSessionCompat.QueueItem> queueItems) {
                        Timber.d("New queue size=%d", queueItems.size());
                        screens.clear();
                        for (MediaSessionCompat.QueueItem item : queueItems) {
                            screens.add(new FooterPageScreen(item));
                        }
                        screens.trimToSize();
                        if (hasView()) {
                            getView().onNewItems(screens);
                        }
                        updatePagerWithCurrentItem(lastPlayingId);
                        selfChange = false;
                    }
                }
        );
        broadcastSubscriptions = new CompositeSubscription(s1, s2);
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }
    }

}
