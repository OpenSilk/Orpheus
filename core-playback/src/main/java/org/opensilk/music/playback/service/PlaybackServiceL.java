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

package org.opensilk.music.playback.service;

import android.annotation.TargetApi;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.service.media.MediaBrowserService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.opensilk.common.core.mortar.DaggerService;
import org.opensilk.music.playback.PlaybackComponent;

import java.util.List;

import javax.inject.Inject;

import hugo.weaving.DebugLog;
import rx.Scheduler;

/**
 * Created by drew on 10/18/15.
 */
@TargetApi(21)
public class PlaybackServiceL extends MediaBrowserService implements PlaybackServiceProxy {

    @Inject PlaybackService mPlaybackService;

    @Override
    public void onCreate() {
        PlaybackComponent parent = DaggerService.getDaggerComponent(getApplicationContext());
        parent.playbackServiceComponent(PlaybackServiceModule.create(this)).inject(this);
        super.onCreate();
        mPlaybackService.onCreate(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPlaybackService.onDestroy();
    }

    @Override
    @DebugLog
    public IBinder onBind(Intent intent) {
        mPlaybackService.onBind();
        return super.onBind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mPlaybackService.onUnbind();
        return super.onUnbind(intent);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mPlaybackService.onTrimMemory(level);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return mPlaybackService.onStartCommand(intent, flags, startId);
    }

    @Override
    public void setSessionToken(Object token) {
        super.setSessionToken((MediaSession.Token) token);
    }

    @Override
    public void startSelf() {
        startService(new Intent(PlaybackServiceL.this, PlaybackServiceL.class));
    }

    @Override
    public MediaSessionHolder getSessionHolder() {
        return mPlaybackService.getSessionHolder();
    }

    @Override
    public Handler getHandler() {
        return mPlaybackService.getHandler();
    }

    @Override
    public Scheduler getScheduler() {
        return mPlaybackService.getScheduler();
    }

    @Nullable @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, Bundle rootHints) {
        return mPlaybackService.getIndexClient().browserGetRootL(clientPackageName, clientUid, rootHints);
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowser.MediaItem>> result) {
        mPlaybackService.getIndexClient().browserLoadChildrenL(parentId, result);
    }
}
