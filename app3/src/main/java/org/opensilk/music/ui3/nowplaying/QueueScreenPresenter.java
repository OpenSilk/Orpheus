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
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat.QueueItem;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.PopupMenu;

import org.opensilk.common.core.dagger2.ScreenScope;
import org.opensilk.common.ui.mortar.PauseAndResumeRegistrar;
import org.opensilk.common.ui.mortar.PausesAndResumes;
import org.opensilk.music.artwork.requestor.ArtworkRequestManager;
import org.opensilk.music.playback.control.PlaybackController;
import org.opensilk.music.ui3.dragswipe.DragSwipeRecyclerAdapter;
import org.opensilk.music.ui3.common.OverflowAction;
import org.opensilk.music.ui3.main.MainPresenter;

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
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        if (pauseAndResumeRegistrar.isRunning()) {
            subscribeBroadcasts();
        }
    }

    @Override
    protected void onSave(Bundle outState) {
        super.onSave(outState);
        if (pauseAndResumeRegistrar.isRunning()) {
            unsubscribeBroadcasts();
        }
    }

    @Override
    public void onResume() {
        if (hasView()) {
            subscribeBroadcasts();
        }
    }

    @Override
    public void onPause() {
        unsubscribeBroadcasts();
    }

    @DebugLog
    public void onItemClicked(Context context, QueueItem item) {
    }

    public void onOverflowClicked(Context context, PopupMenu m, QueueItem item) {
    }

    public boolean onOverflowActionClicked(Context context, OverflowAction action, QueueItem item) {
        return false;
    }

    void subscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            return;
        }
        Subscription s = playbackController.subscribeMetaChanges(
                new Action1<MediaMetadataCompat>() {
                    @Override
                    public void call(MediaMetadataCompat mediaMetadata) {
                        String track = mediaMetadata.getString(METADATA_KEY_TITLE);
                        String artist = mediaMetadata.getString(METADATA_KEY_ARTIST);
                        String id = mediaMetadata.getString(METADATA_KEY_MEDIA_ID);
                        if (hasView()) {
                            getView().mTitle.setText(track);
                            getView().mSubTitle.setText(artist);
                            getView().getAdapter().setActiveItem(id);
                        }
                    }
                }
        );
        Subscription s1 = playbackController.subscribePlayStateChanges(
                new Action1<PlaybackStateCompat>() {
                    @Override
                    public void call(PlaybackStateCompat playbackState) {
                        if (hasView()) {
                            getView().getAdapter().setPlaying(MainPresenter.isPlaying(playbackState));
                        }
                    }
                }
        );
        Subscription s2 = playbackController.subscribeQueueChanges(
                new Action1<List<QueueItem>>() {
                    @Override
                    public void call(List<QueueItem> queueItems) {
                        if (hasView()) {
                            getView().getAdapter().replaceAll(queueItems);
                        }
                    }
                }
        );
        broadcastSubscriptions = new CompositeSubscription(s, s1, s2);
    }

    void unsubscribeBroadcasts() {
        if (isSubscribed(broadcastSubscriptions)) {
            broadcastSubscriptions.unsubscribe();
            broadcastSubscriptions = null;
        }
    }

}
