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

package org.opensilk.music.dream;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.R;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.artwork.ArtworkImageView;
import org.opensilk.music.artwork.ArtworkManager;
import java.lang.reflect.Field;

import hugo.weaving.DebugLog;

/**
 * Created by drew on 4/4/14.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class DayDreamService extends DreamService {
    private static final String TAG = DayDreamService.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;


    private View mContentView, mSaverView;

    // True if attached to window
    private boolean isAttached;

    // Music service token
    private MusicUtils.ServiceToken mMusicServiceToken;
    // True if bound to music service
    private boolean isBoundToMusicService;
    // True if bount to alt dream service
    private boolean isBoundToAltDream;

    private Handler mHandler = new Handler();

    private ScreenSaverAnimation mMoveSaverRunnable = null;

    @DebugLog
    @Override
    public void onCreate() {
        super.onCreate();
        mMusicServiceToken = MusicUtils.bindToService(this, mMusicServiceConnection);
        mMoveSaverRunnable = new ScreenSaverAnimation(mHandler);
    }

    @DebugLog
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isBoundToMusicService) {
            MusicUtils.unbindFromService(mMusicServiceToken);
            isBoundToMusicService = false;
        }
        if (isBoundToAltDream) {
            unbindService(mAltDreamConnection);
            isBoundToAltDream = false;
        }
    }

    @DebugLog
    @Override
    public void onAttachedToWindow() {
        isAttached = true;
        super.onAttachedToWindow();
        if (isBoundToMusicService) {
            if (!MusicUtils.isPlaying()) {
                bindAltDream();
            } else {
                mHandler.removeCallbacks(mMoveSaverRunnable);
                setupSaverView();
                mHandler.post(mMoveSaverRunnable);
            }
        }
    }

    @DebugLog
    @Override
    public void onDetachedFromWindow() {
        isAttached = false;
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mMoveSaverRunnable);
    }

    /**
     * Init our dreams view
     */
    @DebugLog
    private void setupSaverView() {
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        setContentView(R.layout.daydream_container);

        mSaverView = findViewById(R.id.content_wrapper);
        mSaverView.setAlpha(0);
        mContentView = (View) mSaverView.getParent();

        LayoutInflater inflater = getWindow().getLayoutInflater();

        //TODO: Settings for different views

        inflater.inflate(R.layout.daydream_album_art_only, (LinearLayout) mSaverView, true);
        ArtworkImageView artwork = (ArtworkImageView) findViewById(R.id.album_art_view);
        ArtworkManager.loadCurrentArtwork(artwork);

        mMoveSaverRunnable.registerViews(mContentView, mSaverView);
    }

    /**
     * Performs bind on alt dream service
     */
    @DebugLog
    private void bindAltDream() {
        ComponentName altDream = AlternateDreamFragment.getAltDreamComponent(this);
        if (altDream != null) {
            Intent intent = new Intent(DreamService.SERVICE_INTERFACE)
                    .setComponent(altDream)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            bindService(intent, mAltDreamConnection, BIND_AUTO_CREATE);
        } else {
            Log.w(TAG, "Alternate dream not set");
            setupSaverView();
        }
    }

    /**
     * Music service connection
     */
    final ServiceConnection mMusicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBoundToMusicService = true;
            if (isAttached) {
                if (!MusicUtils.isPlaying()) {
                    bindAltDream();
                } else {
                    setupSaverView();
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBoundToMusicService = false;
        }
    };

    /**
     * Alt dream service connection
     */
    final ServiceConnection mAltDreamConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBoundToAltDream = true;
            try {
                IDreamService dreamService = IDreamService.Stub.asInterface(service);
                // Get super
                Class s = DayDreamService.class.getSuperclass();
                // pull out the WindowToken
                // this is the token passed from the DreamManager
                Field f = s.getDeclaredField("mWindowToken");
                f.setAccessible(true);
                // extract the actual IBinder
                IBinder token = (IBinder) f.get(DayDreamService.this);
                // forward our token to the alt dream service
                dreamService.attach(token);
            } catch (NoSuchFieldException|IllegalAccessException|NullPointerException|RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBoundToAltDream = false;
        }
    };
}
