/*
 * Copyright (C) 2014 OpenSilk Productions LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opensilk.music.appwidgets;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.andrew.apollo.MusicPlaybackService;
import com.andrew.apollo.utils.MusicUtils;

import java.util.ArrayDeque;
import java.util.Deque;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 3/31/14.
 */
public class MusicWidgetService extends Service implements ServiceConnection {

    public static final String QUERY_MUSIC = MusicPlaybackService.APOLLO_PACKAGE_NAME + ".QUERY_MUSIC";
    public static final String QUERY_RESPONSE = MusicPlaybackService.APOLLO_PACKAGE_NAME + ".QUERY_RESPONSE";

    private boolean isBound = false;
    private MusicUtils.ServiceToken mToken;

    @Override
    public IBinder onBind(Intent intent) {
        return null; //Not bindable
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mToken = MusicUtils.bindToService(this, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MusicUtils.unbindFromService(mToken);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(QUERY_MUSIC)) {
            final int widgetId = intent.getIntExtra("widget_id", -1);
            if (widgetId != -1) {
                if (isBound) {
                    updateWidgets();
                } //else wait for service bind
            }
        }
        return START_NOT_STICKY;
    }

    private void updateWidgets() {
        final Intent intent = new Intent(QUERY_RESPONSE);
        intent.putExtra("album_id", MusicUtils.getCurrentAlbumId());
        intent.putExtra("artist", MusicUtils.getArtistName());
        intent.putExtra("album", MusicUtils.getAlbumName());
        intent.putExtra("track", MusicUtils.getTrackName());
        intent.putExtra("playing", MusicUtils.isPlaying());
        intent.putExtra("isfavorite", MusicUtils.isFavorite());
        intent.putExtra("repeat", MusicUtils.getRepeatMode());
        intent.putExtra("shuffle", MusicUtils.getShuffleMode());
        sendStickyBroadcast(intent);
        stopSelf();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        isBound = true;
        updateWidgets();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
        mToken = null;
        //TODO maybe rebind
    }

}