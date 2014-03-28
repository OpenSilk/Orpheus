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

package org.opensilk.music.artwork;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.andrew.apollo.BuildConfig;

import hugo.weaving.DebugLog;

/**
 * Proxy for remote processes to access the ArtworkManager
 *
 * As of now the easiest way i know of to implement an IBinder
 * is to just use a service, Ergo this class exists
 *
 * Created by drew on 3/23/14.
 */
public class ArtworkService extends Service {
    private static final String TAG = ArtworkService.class.getSimpleName();
    private static final boolean D = BuildConfig.DEBUG;

    public static final String ACTION_CLEAR_CACHE = "clear_cache";
    private static final int TWO_MINUTES = 2 * 60 * 1000;

    IArtworkServiceImpl mRemoteBinder;
    ArtworkManager mManager;
    Handler mHandler;

    @Override
    public IBinder onBind(Intent intent) {
        return mRemoteBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    @DebugLog
    public void onCreate() {
        super.onCreate();
        mRemoteBinder = new IArtworkServiceImpl(this);
        mManager = ArtworkManager.getInstance(getApplicationContext());
        mHandler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && ACTION_CLEAR_CACHE.equals(intent.getAction())) {
            if (D) Log.d(TAG, "Queueing request to clear mem cache");
            mHandler.postDelayed(mClearCacheTask, TWO_MINUTES);
        } else {
            if (D) Log.d(TAG, "Canceling request to clear mem cache");
            mHandler.removeCallbacks(mClearCacheTask);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRemoteBinder = null;
        mManager = null;
        ArtworkManager.destroy();
    }

    private final Runnable mClearCacheTask = new Runnable() {
        @Override
        public void run() {
            if (mManager != null) {
                Log.d(TAG, "Clearing mem cache");
                mManager.mL1Cache.evictAll();
            }
        }
    };

}
