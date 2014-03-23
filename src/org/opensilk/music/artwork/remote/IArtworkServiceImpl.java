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

package org.opensilk.music.artwork.remote;

import android.graphics.Bitmap;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.andrew.apollo.BuildConfig;
import com.andrew.apollo.model.Album;
import com.andrew.apollo.utils.MusicUtils;

import org.opensilk.music.artwork.ArtworkLoader;
import org.opensilk.music.artwork.cache.CacheUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import hugo.weaving.DebugLog;

import static com.andrew.apollo.ApolloApplication.sDefaultMaxImageWidthPx;

/**
 * IArtworkService implementation
 *
 * Created by drew on 3/23/14.
 */
public class IArtworkServiceImpl extends IArtworkService.Stub {
    private static final String TAG = "IArtworkServiceImpl";
    private static final boolean D = BuildConfig.DEBUG;

    private WeakReference<ArtworkService> mService;
    private File mTempDir;

    public IArtworkServiceImpl(ArtworkService service) {
        mService = new WeakReference<>(service);
        mTempDir =  CacheUtil.getCacheDir(service.getContext(), "tmp-artproxy");
        if (mTempDir != null && !mTempDir.exists()) {
            mTempDir.mkdirs();
        }
    }

    @Override
    @DebugLog
    public String getCurrentArtwork() throws RemoteException {
        ArtworkService service = mService.get();
        if (service != null) {
            Album album = MusicUtils.getCurrentAlbum(service.getContext());
            if (album != null) {
                String cacheKey = ArtworkLoader.getCacheKey(album.mArtistName, album.mAlbumName, sDefaultMaxImageWidthPx, 0);
                if (D) Log.d(TAG, "Checking caches for " + cacheKey);
                Bitmap bitmap =  service.getL1Cache().getBitmap(cacheKey);
                if (bitmap == null) {
                    bitmap = service.getL2Cache().getBitmap(cacheKey);
                }
                if (bitmap != null) {
                    FileOutputStream os = null;
                    File tempFile = null;
                    try {
                        // Create tempfile
                        tempFile = File.createTempFile("tmp-", ".img", mTempDir);
                        // Write the bitmap to the temp file
                        os = new FileOutputStream(tempFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                        os.close();
                        // return the file path TODO pass FileDescriptor instead
                        String path = tempFile.getCanonicalPath();
                        if (!TextUtils.isEmpty(path)) {
                            return path;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if (tempFile != null && tempFile.exists()) {
                            try {
                                tempFile.delete();
                            } catch (Exception ignored) { }
                        }
                    }
                }
                Log.e(TAG, "Requested image not in the cache! " + cacheKey);
            }
        }
        return "";
    }


}
