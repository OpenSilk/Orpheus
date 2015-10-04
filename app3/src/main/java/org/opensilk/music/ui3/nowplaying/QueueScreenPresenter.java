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
import android.media.session.MediaSession;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
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

import static android.support.v4.media.MediaMetadataCompat.*;
import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 5/9/15.
 */
@ScreenScope
public class QueueScreenPresenter extends ViewPresenter<QueueScreenView>
        implements PausesAndResumes {

    final PlaybackController playbackController;
    final ArtworkRequestManager requestor;
    final PauseAndResumeRegistrar pauseAndResumeRegistrar;

    CompositeSubscription broadcastSubscriptions;
    boolean isPlaying;
    long lastPlayingId;
    ArrayList<QueueItem> queue = new ArrayList<>();

    @Inject
    public QueueScreenPresenter(
            PlaybackController playbackController,
            ArtworkRequestManager requestor,
            PauseAndResumeRegistrar pauseAndResumeRegistrar
    ) {
        this.playbackController = playbackController;
        this.requestor = requestor;
        this.pauseAndResumeRegistrar = pauseAndResumeRegistrar;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        pauseAndResumeRegistrar.register(scope, this);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        teardown();
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            setup();
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (pauseAndResumeRegistrar.isRunning()) {
            teardown();
        }
    }

    @Override
    public void onResume() {
        setup();
    }

    @Override
    public void onPause() {
        teardown();
    }

    void setup() {
        if (hasView()) {
            subscribeBroadcasts();
        }
    }

    void teardown() {
        unsubscribeBroadcasts();
    }

    void onItemMoved(int from, int to) {
        playbackController.moveQueueItem(from, to);
    }

    void onItemRemoved(int pos) {
        playbackController.removeQueueItemAt(pos);
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            return;
        }
        Subscription s1 = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        isPlaying = PlaybackStateHelper.isPlaying(playbackState.getState());
                        lastPlayingId = playbackState.getActiveQueueItemId();
                        if (hasView()) {
                            getView().getAdapter().setPlaying(isPlaying);
                            getView().getAdapter().setActiveItem(lastPlayingId);
                        }
                    }
                }
        );
        Subscription s2 = playbackController.subscribeQueueChanges(
                new Action1<List<QueueItem>>() {
                    @Override
                    @DebugLog
                    public void call(List<QueueItem> queueItems) {
                        queue.clear();
                        queue.addAll(queueItems);
                        queue.trimToSize();
                        if (hasView()) {
                            getView().getAdapter().replaceAll(queue);
                            getView().getAdapter().setActiveItem(lastPlayingId);
                        }
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
