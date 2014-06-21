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

package org.opensilk.music.muzei;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.andrew.apollo.utils.MusicUtils;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;

import org.opensilk.music.api.model.Album;
import org.opensilk.music.api.model.ArtInfo;
import org.opensilk.music.artwork.ArtworkProvider;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 4/16/14.
 */
public class MuzeiService extends MuzeiArtSource implements ServiceConnection {
    private static final String TAG = MuzeiService.class.getSimpleName();

    public static final String MUZEI_EXTENSION_ENABLED = "is_muzei_enabled";

    private MusicUtils.ServiceToken mToken;
    private boolean isBound;

    public MuzeiService() {
        this ("Orpheus");
    }

    public MuzeiService(String name) {
        super(name);
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
    @DebugLog
    public void onEnabled() {
        // Tells the receiver its ok to send us updates
        // Note: can't simply enable/disable the receiver because
        // muzei detects package changes and doing so will result
        // in an endless loop on enable/disable
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(MUZEI_EXTENSION_ENABLED, true).apply();
    }

    @Override
    @DebugLog
    public void onDisabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(MUZEI_EXTENSION_ENABLED, false).apply();
    }

    @Override
    @DebugLog
    protected void onUpdate(int reason) {
        if (!isBound && !waitForBind()) {
            return;
        }
        final ArtInfo info = MusicUtils.getCurrentArtInfo();
        if (info == null) {
            Timber.e("Nothing currently playing");
            return;
        }
        final Uri artworUri = ArtworkProvider.createArtworkUri(info.artistName, info.albumName);
        publishArtwork(new Artwork.Builder()
                .imageUri(artworUri)
                .title(MusicUtils.getAlbumName())
                .byline(MusicUtils.getArtistName())
                .build());
    }

    private synchronized boolean waitForBind() {
        try {
            long waitTime = 0;
            while (!isBound) {
                // Don' block for more than a second
                if (waitTime > 1000) return false;
                Log.i(TAG, "Waiting on service");
                // We were called too soon after onCreate, give the service some time
                // to spin up, This is run in a Handler thread so we can block it
                long start = System.currentTimeMillis();
                wait(100);
                Log.i(TAG, "Waited for " + (waitTime += (System.currentTimeMillis() - start)) + "ms");
            }
        } catch (InterruptedException e) {
            return false;
        }
        return isBound;
    }

    /*
     * Service Connection callbacks
     */

    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        isBound = true;
        notifyAll();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
    }

}
