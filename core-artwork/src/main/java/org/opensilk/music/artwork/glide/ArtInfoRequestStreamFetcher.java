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

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import org.opensilk.music.model.ArtInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import hugo.weaving.DebugLog;
import timber.log.Timber;

/**
 * Created by drew on 12/25/15.
 */
public class ArtInfoRequestStreamFetcher implements DataFetcher<InputStream> {

    final Context context;
    final ArtInfo artInfo;
    final String authority;

    InputStream data;

    public ArtInfoRequestStreamFetcher(Context context, ArtInfoRequest artInfoRequest) {
        this.context = context;
        this.artInfo = artInfoRequest.artInfo;
        this.authority = artInfoRequest.authority;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super InputStream> callback) {
        ContentResolver contentResolver = context.getContentResolver();
        try {
            final Uri uri = artInfo.asContentUri(authority);
            InputStream inputStream = contentResolver.openInputStream(uri);
            if (inputStream == null) {
                throw new FileNotFoundException("InputStream is null for :" + uri);
            }
            data = inputStream;
        } catch (FileNotFoundException e) {
            Timber.w("loadData(%s) Failed to open", artInfo);
            callback.onLoadFailed(e);
            return;
        }
        callback.onDataReady(data);
    }

    @Override
    public void cleanup() {
        if (data != null) {
            try {
                data.close();
            } catch (IOException e) {
                Timber.w("close(%s) %s", artInfo, e.getMessage());
            }
        }
    }

    @Override
    public void cancel() {
    }

    @Override
    public Class<InputStream> getDataClass() {
        return InputStream.class;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
