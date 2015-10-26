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

import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.MenuItem;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.Lifecycle;
import org.opensilk.common.ui.mortar.LifecycleService;
import org.opensilk.music.R;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.PlaybackStateHelper;
import org.opensilk.music.playback.control.PlaybackController;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import mortar.MortarScope;
import mortar.ViewPresenter;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.subscriptions.CompositeSubscription;
import timber.log.Timber;

import static org.opensilk.common.core.rx.RxUtils.isSubscribed;

/**
 * Created by drew on 5/9/15.
 */
@ScreenScope
public class QueueScreenPresenter extends ViewPresenter<QueueScreenView> {

    final PlaybackController playbackController;
    final ArtworkRequestManager requestor;

    Observable<Lifecycle> lifecycle;
    CompositeSubscription broadcastSubscriptions;
    Subscription lifcycleSubscripton;

    boolean isPlaying;
    long lastPlayingId;
    boolean selfChange;
    ArrayList<QueueItem> queue = new ArrayList<>();

    @Inject
    public QueueScreenPresenter(
            PlaybackController playbackController,
            ArtworkRequestManager requestor
    ) {
        this.playbackController = playbackController;
        this.requestor = requestor;
    }

    @Override
    protected void onEnterScope(MortarScope scope) {
        super.onEnterScope(scope);
        lifecycle = LifecycleService.getLifecycle(scope);
    }

    @Override
    protected void onExitScope() {
        super.onExitScope();
        if (lifcycleSubscripton != null) {
            lifcycleSubscripton.unsubscribe();
        }
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (!queue.isEmpty()) {
            getView().getAdapter().addAll(queue);
            getView().getAdapter().setActiveItem(lastPlayingId);
            getView().getAdapter().setPlaying(isPlaying);
        }
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
                        break;
                    }
                    case PAUSE: {
                        unsubscribeBroadcasts();
                        break;
                    }
                }
            }
        });
    }

    void onItemMoved(int from, int to) {
        selfChange = true;
        playbackController.moveQueueItem(from, to);
    }

    void onItemRemoved(int pos) {
        selfChange = true;
        playbackController.removeQueueItemAt(pos);
    }

    void onItemClicked(QueueItem item) {
        playbackController.skipToQueueItem(item.getQueueId());
    }

    public boolean onMenuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save_queue:
                return true;
            case R.id.clear_queue:
                playbackController.clearQueue();
                return true;
            default:
                return false;
        }
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
                        boolean needupdate = true;
                        if (selfChange) {
                            selfChange = false;
                            if (hasView() && getView().getAdapter().getItemCount() == queueItems.size()) {
                                needupdate = false;
                                Iterator<QueueItem> i1 = getView().getAdapter().getItems().iterator();
                                Iterator<QueueItem> i2 = queueItems.iterator();
                                while (i1.hasNext() && i2.hasNext()) {
                                    if (i1.next().getQueueId() != i2.next().getQueueId()) {
                                        needupdate = true;
                                        break;
                                    }
                                }
                            }
                        }
                        queue.clear();
                        queue.addAll(queueItems);
                        queue.trimToSize();
                        if (hasView()) {
                            if (needupdate) {
                                Timber.d("Replacing queue");
                                if (queue.isEmpty()) {
                                    getView().getAdapter().clear();
                                } else {
                                    getView().getAdapter().replaceAll(queue);
                                }
                            }
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
