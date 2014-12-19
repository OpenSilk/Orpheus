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
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.opensilk.common.util.VersionUtils;
import org.opensilk.music.AppModule;
import org.opensilk.music.R;
import org.opensilk.music.dream.views.ArtOnly;
import org.opensilk.music.dream.views.ArtWithControls;
import org.opensilk.music.dream.views.ArtWithMeta;
import org.opensilk.music.dream.views.VisualizerWave;
import org.opensilk.music.ui2.core.BroadcastObservables;
import org.opensilk.music.MusicServiceConnection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Provides;
import de.greenrobot.event.EventBus;
import mortar.Mortar;
import mortar.MortarActivityScope;
import rx.Subscription;
import rx.functions.Action1;
import timber.log.Timber;

import static org.opensilk.common.rx.RxUtils.isSubscribed;
import static org.opensilk.common.rx.RxUtils.observeOnMain;

/**
 * Created by drew on 4/4/14.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class DayDreamService extends DreamService {

    public static class Blueprint implements mortar.Blueprint {
        @Override
        public String getMortarScopeName() {
            return getClass().getName();
        }

        @Override
        public Object getDaggerModule() {
            return new Module();
        }

    }

    @dagger.Module(
            addsTo = AppModule.class,
            injects = {
                    DayDreamService.class,
                    ArtOnly.class,
                    ArtWithControls.class,
                    ArtWithMeta.class,
                    VisualizerWave.class,
            }
    )
    public static class Module {
        @Provides @Singleton @Named("activity")
        public EventBus provideEventBus() {
            return new EventBus();
        }
    }

    @Inject MusicServiceConnection mServiceConnection;
    @Inject DreamPrefs mDreamPrefs;

    final Handler mHandler;
    final ScreenSaverAnimation mMoveSaverRunnable;

    MortarActivityScope mDreamScope;
    Subscription playStateSubscription;
    ViewGroup mContentView, mSaverView, mDreamView;
    LayoutInflater mLayoutInflater;

    boolean isAttached;
    boolean isBoundToAltDream;
    boolean isPlaying;

    public DayDreamService() {
        mHandler = new Handler();
        mMoveSaverRunnable = new ScreenSaverAnimation(mHandler);
    }

    //@DebugLog
    @Override
    public void onCreate() {
        super.onCreate();
        // Sort of a hack, we dont have a persistence bundle but we do have a window and mortar
        // requires activity scopes for presenters.
        mDreamScope = Mortar.requireActivityScope(Mortar.getScope(getApplication()), new Blueprint());
        mDreamScope.onCreate(null);
        Mortar.inject(this, this);
        mServiceConnection.bind();
        playStateSubscription = BroadcastObservables.playStateChanged(this).subscribe(new Action1<Boolean>() {
            @Override
            public void call(Boolean playing) {
                isPlaying = playing;
                if (playing && isBoundToAltDream) {
                    switchToSaverView();
                }
            }
        });
    }

    //@DebugLog
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (isSubscribed(playStateSubscription)) {
            playStateSubscription.unsubscribe();
            playStateSubscription = null;
        }
        mServiceConnection.unbind();
        if (isBoundToAltDream) {
            isBoundToAltDream = false;
            unbindService(mAltDreamConnection);
        }
        Mortar.getScope(getApplication()).destroyChild(mDreamScope);
    }

    //@DebugLog
    @Override
    public void onAttachedToWindow() {
        isAttached = true;
        super.onAttachedToWindow();
        if (!isPlaying) {
            bindAltDream();
        } else {
            setupSaverView();
        }
    }

    //@DebugLog
    @Override
    public void onDetachedFromWindow() {
        isAttached = false;
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mMoveSaverRunnable);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (!isBoundToAltDream) {
            setupSaverView();
        }
    }

    @Override
    public Object getSystemService(String name) {
        if (Mortar.isScopeSystemService(name)) {
            return mDreamScope;
        }
        // We want the child views to be injectable from our scope
        if (LAYOUT_INFLATER_SERVICE.equals(name)) {
            if (mLayoutInflater == null) {
                mLayoutInflater = LayoutInflater.from(getBaseContext()).cloneInContext(this);
            }
            return mLayoutInflater;
        }
        return super.getSystemService(name);
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

        setScreenBright(!mDreamPrefs.wantNightMode());
        setFullscreen(mDreamPrefs.wantFullscreen());
        setInteractive(false);

        LayoutInflater inflater = getWindow().getLayoutInflater();
        int style = mDreamPrefs.getDreamLayout();
        switch (style) {
            case DreamPrefs.DreamLayout.ART_ONLY:
                mDreamView = (ViewGroup) inflater.inflate(R.layout.daydream_art_only, mSaverView, false);
                break;
            case DreamPrefs.DreamLayout.ART_META:
                mDreamView = (ViewGroup) inflater.inflate(R.layout.daydream_art_meta, mSaverView, false);
                break;
            case DreamPrefs.DreamLayout.ART_CONTROLS:
                mDreamView = (ViewGroup) inflater.inflate(R.layout.daydream_art_controls, mSaverView, false);
                setInteractive(true);
                break;
            case DreamPrefs.DreamLayout.VISUALIZER_WAVE:
                mDreamView = (ViewGroup) inflater.inflate(R.layout.daydream_visualizer_wave, mSaverView, false);
                break;
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
        ComponentName altDream = mDreamPrefs.getAltDreamComponent();
        if (altDream != null) {
            Intent intent = new Intent(DreamService.SERVICE_INTERFACE)
                    .setComponent(altDream)
                    .addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            try {
                bindService(intent, mAltDreamConnection, BIND_AUTO_CREATE);
            } catch (SecurityException e) {
                Timber.w("Altdream: %s requires permission we can't obtain", altDream.flattenToString());
                mDreamPrefs.removeAltDreamComponent();
                switchToSaverView();
            }
        } else {
            setupSaverView();
        }
    }

    void switchToSaverView() {
        isBoundToAltDream = false;
        unbindService(mAltDreamConnection);
        setupSaverView();
    }

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
                if (VersionUtils.hasLollipop()) {
                    Method attach = iDreamService.getDeclaredMethod("attach", IBinder.class, boolean.class);
                    attach.invoke(dreamService, token, false);
                } else {
                    Method attach = iDreamService.getDeclaredMethod("attach", IBinder.class);
                    attach.invoke(dreamService, token);
                }
            } catch (Exception e) {
                Timber.w(e, "Failed attaching to altDream");
                switchToSaverView();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            switchToSaverView();
        }
    };

}
