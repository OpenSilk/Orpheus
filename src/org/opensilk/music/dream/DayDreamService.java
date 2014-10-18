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
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.opensilk.music.BuildConfig;
import org.opensilk.music.R;
import com.andrew.apollo.utils.MusicUtils;
import com.squareup.otto.Subscribe;

import org.opensilk.music.bus.EventBus;
import org.opensilk.music.bus.events.MetaChanged;
import org.opensilk.music.bus.events.PlaybackModeChanged;
import org.opensilk.music.bus.events.PlaystateChanged;
import org.opensilk.music.dream.views.IDreamView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by drew on 4/4/14.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class DayDreamService extends DreamService {
    private static final String TAG = DayDreamService.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    private ViewGroup mContentView, mSaverView, mDreamView;

    // True if attached to window
    private boolean isAttached;

    // Music service token
    private MusicUtils.ServiceToken mMusicServiceToken;
    // True if bound to music service
    private boolean isBoundToMusicService;
    // True if bount to alt dream service
    private boolean isBoundToAltDream;

    private final Handler mHandler;
    private final ScreenSaverAnimation mMoveSaverRunnable;

    public DayDreamService() {
        mHandler = new Handler();
        mMoveSaverRunnable = new ScreenSaverAnimation(mHandler);
    }

    //@DebugLog
    @Override
    public void onCreate() {
        super.onCreate();
        mMusicServiceToken = MusicUtils.bindToService(this, mMusicServiceConnection);
    }

    //@DebugLog
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

    //@DebugLog
    @Override
    public void onAttachedToWindow() {
        isAttached = true;
        super.onAttachedToWindow();
        if (isBoundToMusicService) {
            if (!MusicUtils.isPlaying()) {
                bindAltDream();
            } else {
                setupSaverView();
            }
        }
        EventBus.getInstance().register(this);
    }

    //@DebugLog
    @Override
    public void onDetachedFromWindow() {
        isAttached = false;
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mMoveSaverRunnable);
        EventBus.getInstance().unregister(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!isBoundToAltDream) {
            setupSaverView();
        }
    }

    /**
     * Init our dreams view
     */
    //@DebugLog
    private void setupSaverView() {
        mHandler.removeCallbacks(mMoveSaverRunnable);

        setContentView(R.layout.daydream_container);
        mSaverView = (ViewGroup) findViewById(R.id.dream_container);
        mSaverView.setAlpha(0);
        mContentView = (ViewGroup) mSaverView.getParent();

        setScreenBright(!DreamPrefs.wantNightMode(this));
        setFullscreen(DreamPrefs.wantFullscreen(this));

        LayoutInflater inflater = getWindow().getLayoutInflater();
        int style = DreamPrefs.getDreamLayout(this);
        switch (style) {
            case DreamPrefs.DreamLayout.ART_ONLY:
                mDreamView = (ViewGroup) inflater.inflate(R.layout.daydream_art_only, mSaverView, false);
                setInteractive(false);
                break;
            case DreamPrefs.DreamLayout.ART_META:
                mDreamView = (ViewGroup) inflater.inflate(R.layout.daydream_art_meta, mSaverView, false);
                setInteractive(false);
                break;
            case DreamPrefs.DreamLayout.ART_CONTROLS:
                mDreamView = (ViewGroup) inflater.inflate(R.layout.daydream_art_controls, mSaverView, false);
                setInteractive(true);
        }
        if (mDreamView != null) {
            mSaverView.addView(mDreamView);
        }
        mMoveSaverRunnable.registerViews(mContentView, mSaverView);

        mHandler.post(mMoveSaverRunnable);
    }

    /**
     * Performs bind on alt dream service
     */
    //@DebugLog
    private void bindAltDream() {
        ComponentName altDream = DreamPrefs.getAltDreamComponent(this);
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
                // Bind the service
                Class stub = Class.forName("android.service.dreams.IDreamService$Stub");
                Method asIface = stub.getDeclaredMethod("asInterface", IBinder.class);
                Object dreamService = asIface.invoke(null, service);
                // Get super
                Class s = DayDreamService.class.getSuperclass();
                // pull out the WindowToken, this is the token passed from the DreamManager
                Field windowToken = s.getDeclaredField("mWindowToken");
                windowToken.setAccessible(true);
                // extract the actual IBinder
                IBinder token = (IBinder) windowToken.get(DayDreamService.this);
                // forward our token to the alt dream service
                Class iDreamService = Class.forName("android.service.dreams.IDreamService");
                Method attach = iDreamService.getDeclaredMethod("attach", IBinder.class);
                attach.invoke(dreamService, token);
            } catch (Exception e) {
                e.printStackTrace();
                setupSaverView();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBoundToAltDream = false;
        }
    };

    /*
     * Events
     */

    @Subscribe
    public void onMetaChanged(MetaChanged e) {
        updateView();
    }

    @Subscribe
    public void onPlaystateChanged(PlaystateChanged e) {
        updateView();
    }

    @Subscribe
    public void onPlaybackModeChanged(PlaybackModeChanged e) {
        updateView();
    }

    private void updateView() {
        if (mDreamView != null) {
            ((IDreamView) mDreamView).update();
        }
    }

}
