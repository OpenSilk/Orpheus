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

package org.opensilk.music.artwork.glide;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.CancellationSignal;

import com.bumptech.glide.Priority;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

/**
 * Created by drew on 12/25/15.
 */
@TargetApi(19)
public class ArtInfoRequestStreamFetcherK extends ArtInfoRequestStreamFetcher {

    CancellationSignal cancellationSignal;

    public ArtInfoRequestStreamFetcherK(Context context, ArtInfoRequest artInfoRequest) {
        super(context, artInfoRequest);
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
        Timber.d("loadData(%s)", artInfo);
        ContentResolver contentResolver = context.getContentResolver();
        try {
            final Uri uri = artInfo.asContentUri(authority);
            cancellationSignal = new CancellationSignal();
            AssetFileDescriptor assetFileDescriptor = contentResolver.openAssetFileDescriptor(uri, "r", cancellationSignal);
            if (assetFileDescriptor == null) {
                throw new FileNotFoundException("FileDescriptor is null for: " + uri);
            }
            data = assetFileDescriptor.createInputStream();
        } catch (IOException e) {
            Timber.w("loadData(%s) Failed to open", artInfo);
            callback.onLoadFailed(e);
            return;
        }
        callback.onDataReady(data);
    }

    @Override
    public void cancel() {
        Timber.d("cancel(%s)", artInfo);
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
        }
    }

}
