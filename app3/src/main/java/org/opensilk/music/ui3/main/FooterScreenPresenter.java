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

import org.apache.commons.lang3.tuple.Triple;
import org.opensilk.common.core.dagger2.ForApplication;
import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.music.AppPreferences;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.control.PlaybackController;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;


/**
 * Created by drew on 4/20/15.
 */
@ScreenScope
public class FooterScreenPresenter extends ViewPresenter<FooterScreenView> implements PausesAndResumes {

    final Context appContext;
    final PlaybackController playbackController;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;
    final AppPreferences settings;

    CompositeSubscription broadcastSubscriptions;
    final ArrayList<FooterPageScreen> screens = new ArrayList<>();
    long lastPlayingId;

    final ProgressUpdater mProgressUpdater = new ProgressUpdater(new Action1<Integer>() {
        @Override
        public void call(Integer integer) {
            setProgress(integer);
        }
    });

    @Inject
    public FooterScreenPresenter(
            @ForApplication Context context,
            PlaybackController playbackController,
            PauseAndResumeRegistrar pauseAndResumeRegistrar,
            AppPreferences settings
    ) {
        this.appContext = context;
        this.playbackController = playbackController;
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
        Timber.v("onExitScope()");
        super.onExitScope();
        teardown();
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
            teardown();
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
        teardown();
    }

    void init() {
        //progress is always updated
        subscribeBroadcasts();
        if(!screens.isEmpty()) {
            getView().onNewItems(screens);
            updatePagerWithCurrentItem(lastPlayingId);
        }
    }

    void teardown() {
        unsubscribeBroadcasts();
        mProgressUpdater.unsubscribeProgress();
    }

    void skipToQueueItem(int pos) {
        if (pos > 0  && pos < screens.size()) {
            long id = screens.get(pos).queueItem.getQueueId();
            playbackController.skipToQueueItem(id);
        }
    }

    void updatePagerWithCurrentItem(long id) {
        if (hasView()) {
            for (FooterPageScreen s : screens) {
                if (s.queueItem.getQueueId() == id) {
                    getView().goTo(screens.indexOf(s));
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
                    @DebugLog
                    public void call(PlaybackStateCompat playbackState) {
                        mProgressUpdater.subscribeProgress(playbackState);
                        lastPlayingId = playbackState.getActiveQueueItemId();
                        updatePagerWithCurrentItem(lastPlayingId);
                    }
                }
        );
        Subscription s2 = playbackController.subscribeQueueChanges(
                new Action1<List<MediaSessionCompat.QueueItem>>() {
                    @Override
                    @DebugLog
                    public void call(List<MediaSessionCompat.QueueItem> queueItems) {
                        screens.clear();
                        for (MediaSessionCompat.QueueItem item : queueItems) {
                            screens.add(new FooterPageScreen(item));
                        }
                        screens.trimToSize();
                        if (hasView()) {
                            getView().onNewItems(screens);
                        }
                        updatePagerWithCurrentItem(lastPlayingId);
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
