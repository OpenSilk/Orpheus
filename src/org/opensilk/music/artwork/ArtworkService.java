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
import android.os.ParcelFileDescriptor;
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
public interface ArtworkService {

    public ParcelFileDescriptor getArtwork(long id);
    public ParcelFileDescriptor getArtworkThumbnail(long id);
//    @Override
//    public void onDestroy() {
//        super.onDestroy();
//        mRemoteBinder = null;
//        mManager = null;
//        mHandler.removeCallbacks(mClearCacheTask);
//        ArtworkManager.destroy();
//    }
//
//    private final Runnable mClearCacheTask = new Runnable() {
//        @Override
//        public void run() {
//            if (mManager != null) {
//                Log.d(TAG, "Clearing mem cache");
//                mManager.mL1Cache.evictAll();
//            }
//        }
//    };

}
